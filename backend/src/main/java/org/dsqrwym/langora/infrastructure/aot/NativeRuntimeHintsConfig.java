package org.dsqrwym.langora.infrastructure.aot;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeRuntimeHintsConfig.AppRuntimeHintsRegistrar.class)
public class NativeRuntimeHintsConfig {

    /**
     * Array classes jOOQ asks for while it registers its built-in data types.
     *
     * <p>jOOQ creates an array data type for every built-in data type while {@code SQLDataType} is
     * being initialised, see the end of {@code org.jooq.impl.DefaultDataType}'s constructor:
     *
     * <pre>{@code
     * // [#13107] Eagerly register array types of built-in types in type registry
     * if (this instanceof BuiltInDataType && !tType.isArray())
     *     getArrayDataType();
     * }</pre>
     *
     * <p>For that it needs the array class itself ({@code AbstractDataType.getArrayType()} -&gt;
     * {@code Internal.arrayType(..)} -&gt; {@code Class.arrayType()}, i.e.
     * {@code java.lang.reflect.Array.newInstance(componentType, 0)}). In a native image this only
     * works for array classes that ended up in the image. An array class that nothing else references
     * is discarded, jOOQ then receives {@code null} and the next statement of that constructor -
     * {@code TYPES_BY_TYPE[ordinal].putIfAbsent(type, this)} - fails, because a
     * {@code ConcurrentHashMap} rejects {@code null} keys:
     *
     * <pre>
     * java.lang.NullPointerException
     *     at java.util.concurrent.ConcurrentHashMap.putVal(ConcurrentHashMap.java:1023)
     *     at java.util.concurrent.ConcurrentHashMap.putIfAbsent(ConcurrentHashMap.java:1555)
     *     at org.jooq.impl.DefaultDataType.&lt;init&gt;(DefaultDataType.java:449)
     *     at org.jooq.impl.DefaultDataType.&lt;init&gt;(DefaultDataType.java:398)
     *     at org.jooq.impl.DefaultDataType.&lt;init&gt;(DefaultDataType.java:374)
     *     at org.jooq.impl.ArrayDataType.&lt;init&gt;(ArrayDataType.java:70)
     *     at org.jooq.impl.AbstractDataType.getArrayDataType(AbstractDataType.java:873)
     *     at org.jooq.impl.DefaultDataType.getArrayDataType(DefaultDataType.java:154)
     *     at org.jooq.impl.DefaultDataType.&lt;init&gt;(DefaultDataType.java:462)
     *     ...
     *     at org.jooq.impl.SQLDataType.&lt;clinit&gt;(SQLDataType.java:326)
     * </pre>
     *
     * <p>The last frame is the trigger: {@code SQLDataType.SMALLINTUNSIGNED}, i.e.
     * {@code org.jooq.types.UShort[]}. Everything initialised before it (String, Boolean, Byte, Short,
     * Integer, Long, BigInteger, UByte, ...) works because some other class in the image references
     * its array class, while {@code UShort[]} is referenced by nothing at all.
     *
     * <p>The GraalVM reachability metadata repository does contain jOOQ metadata, but it was authored
     * for jOOQ 3.20.0 (its index lists tested versions only up to 3.21.6, while this project uses
     * 3.21.7) and registers no array class at all. The list below is derived from the data types
     * declared by jOOQ 3.21.7 ({@code SQLDataType}, {@code SQLDataTypes$ClickHouseDataType},
     * {@code SQLDataTypes$DuckDBDataType}, {@code SQLDataTypes$TrinoDataType} and the
     * {@code org.jooq.util.*DataType} classes of the open source edition). {@code byte[]} needs no
     * entry, {@code DefaultDataType} skips array registration when the value type is already an array.
     */
    private static final String[] JOOQ_BUILT_IN_ARRAY_TYPES = {
            "java.lang.Boolean[]",
            "java.lang.Byte[]",
            "java.lang.Double[]",
            "java.lang.Float[]",
            "java.lang.Integer[]",
            "java.lang.Long[]",
            "java.lang.Object[]",
            "java.lang.Short[]",
            "java.lang.String[]",
            "java.math.BigDecimal[]",
            "java.math.BigInteger[]",
            "java.sql.Date[]",
            "java.sql.Time[]",
            "java.sql.Timestamp[]",
            "java.time.Instant[]",
            "java.time.LocalDate[]",
            "java.time.LocalDateTime[]",
            "java.time.LocalTime[]",
            "java.time.OffsetDateTime[]",
            "java.time.OffsetTime[]",
            "java.time.Year[]",
            "java.util.UUID[]",
            "org.jooq.Decfloat[]",
            "org.jooq.Geography[]",
            "org.jooq.Geometry[]",
            "org.jooq.JSON[]",
            "org.jooq.JSONB[]",
            "org.jooq.Record[]",
            "org.jooq.Result[]",
            "org.jooq.RowId[]",
            "org.jooq.XML[]",
            "org.jooq.types.DayToSecond[]",
            "org.jooq.types.UByte[]",
            "org.jooq.types.UInteger[]",
            "org.jooq.types.ULong[]",
            "org.jooq.types.UShort[]",
            "org.jooq.types.YearToMonth[]",
            "org.jooq.types.YearToSecond[]",
    };

    /**
     * Generated jOOQ record classes.
     *
     * <p>jOOQ never instantiates these classes by a direct {@code new}: whenever a query result is
     * mapped onto a generated record, {@code TableImpl.getRecordConstructor(..)} looks up
     * {@code recordType.getDeclaredConstructor()} and invokes it reflectively. Without this hint a
     * native image fails on the first query that produces a record, e.g.
     * {@code SuperAdminBootstrapRunner.run(..)} -&gt; {@code fetchOneInto(ADMIN_USERS)}:
     *
     * <pre>
     * Caused by: java.lang.IllegalStateException: Could not construct new record
     *     at org.jooq.impl.Tools.recordFactory(Tools.java:1620)
     *     at org.jooq.impl.ResultQueryTrait.fetchOneInto(ResultQueryTrait.java:539)
     * Caused by: java.lang.IllegalStateException: Could not access record constructor
     *     at org.jooq.impl.TableImpl.getRecordConstructor(TableImpl.java:533)
     * Caused by: java.lang.NoSuchMethodException: org.dsqrwym...AdminUsersRecord.&lt;init&gt;()
     *     at java.lang.Class.getDeclaredConstructor(Class.java:2491)
     * </pre>
     *
     * <p>Keep this list in sync with {@code src/main/java/org/dsqrwym/langora/infrastructure/jooq/tables/records}
     * whenever the jOOQ code generator creates a new table.
     */
    private static final String[] JOOQ_GENERATED_RECORD_TYPES = {
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.AdminUserSessionsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.AdminUsersRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.AdmissionApplicationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ArticlesRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ArticleTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.BannersRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.BannerTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.CommentsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ContentCategoriesRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ContentCategoryTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.CountriesRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.MediaAssetsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.MediaAssetTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.OperationLogsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.PagesRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.PageTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ProgramsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.ProgramTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.SiteConfigurationRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.SiteConfigurationTranslationsRecord",
            "org.dsqrwym.langora.infrastructure.jooq.tables.records.SiteLocalesRecord",
    };

    /**
     * Dialect specific data type classes that {@code SQLDataType}'s static initialiser loads through
     * {@code Class.forName(..)}. Without these hints those classes stay unregistered (the initialiser
     * swallows the resulting exception) and SQL rendering degrades, e.g. PostgreSQL timestamps would
     * be rendered as {@code instant} instead of {@code timestamp with time zone}.
     */
    private static final String[] JOOQ_DIALECT_DATA_TYPES = {
            "org.jooq.impl.SQLDataType",
            "org.jooq.impl.SQLDataTypes$ClickHouseDataType",
            "org.jooq.impl.SQLDataTypes$DuckDBDataType",
            "org.jooq.impl.SQLDataTypes$TrinoDataType",
            "org.jooq.util.cubrid.CUBRIDDataType",
            "org.jooq.util.derby.DerbyDataType",
            "org.jooq.util.firebird.FirebirdDataType",
            "org.jooq.util.h2.H2DataType",
            "org.jooq.util.hsqldb.HSQLDBDataType",
            "org.jooq.util.ignite.IgniteDataType",
            "org.jooq.util.mariadb.MariaDBDataType",
            "org.jooq.util.mysql.MySQLDataType",
            "org.jooq.util.postgres.PostgresDataType",
            "org.jooq.util.sqlite.SQLiteDataType",
            "org.jooq.util.yugabytedb.YugabyteDBDataType",
    };

    /**
     * Custom Bean Validation constraint validators.
     *
     * <p>Hibernate Validator never keeps instances of these classes: it asks
     * {@code SpringConstraintValidatorFactory} for one, which calls
     * {@code AutowireCapableBeanFactory.createBean(validatorClass)}. That class is not a regular bean
     * and is only referenced from {@code @Constraint(validatedBy = ..)}, so Spring AOT does not
     * register it. On a native image the no-arg constructor then is not visible to reflection and
     * validation blows up on the first request that uses the constraint - the constructor exists, it
     * is simply unknown to the image:
     *
     * <pre>
     * org.springframework.beans.factory.BeanCreationException: Error creating bean with name
     *   'org.dsqrwym.langora.infrastructure.validation.username.AdminUsernameValidator':
     *   Failed to instantiate [...]: No default constructor found
     * Caused by: java.lang.NoSuchMethodException: ...AdminUsernameValidator.&lt;init&gt;()
     *     at java.lang.Class.getDeclaredConstructor(Class.java:2491)
     *     at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(..)
     *     at org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory.getInstance(..)
     * </pre>
     *
     * <p>Keep this list in sync with {@code org.dsqrwym.langora.infrastructure.validation}; a new
     * {@code ConstraintValidator} implementation must be added here as well (the class itself can
     * alternatively be annotated with {@code @RegisterReflection}).
     */
    private static final String[] APP_CONSTRAINT_VALIDATORS = {
            "org.dsqrwym.langora.infrastructure.validation.email.EmailValidator",
            "org.dsqrwym.langora.infrastructure.validation.password.StrongPasswordValidator",
            "org.dsqrwym.langora.infrastructure.validation.username.AdminUsernameValidator",
    };

    /**
     * Types that Jackson reads/writes as JSON request or response bodies.
     *
     * <p>For records Jackson calls {@code Class.getRecordComponents()}, and a native image refuses
     * that call unless <em>all</em> record component accessor methods are registered - note that the
     * class being reachable is not enough:
     *
     * <pre>
     * com.oracle.svm.core.jdk.UnsupportedFeatureError: Record components not available for record
     *   class org.dsqrwym.langora.infrastructure.web.dto.ValidationErrorDetail. All record component
     *   accessor methods of this record class must be included in the reflection configuration at
     *   image build time, then this method can be called.
     *     at java.lang.Class.getRecordComponents0(DynamicHub.java:1534)
     *     at tools.jackson.databind.util.RecordUtil.getRecordFieldNames(RecordUtil.java:20)
     *     ...
     *     at org.springframework.http.converter.AbstractJacksonHttpMessageConverter.writeInternal(..)
     * </pre>
     *
     * <p>This shows up on the error path first: Spring AOT registers the handler method parameter and
     * return types, but not the types nested inside {@code ApiResponse} produced by
     * {@code GlobalExceptionHandler}. Add every new request/response record here (an alternative is
     * {@code @RegisterReflectionForBinding} on the DTO or on a configuration class).
     */
    private static final String[] APP_JSON_PAYLOAD_TYPES = {
            "org.dsqrwym.langora.audit.OperationDto",
            "org.dsqrwym.langora.auth.dto.LoginRequestDto",
            "org.dsqrwym.langora.auth.dto.LoginResponseDto",
            "org.dsqrwym.langora.auth.dto.RefreshTokenResponse",
            "org.dsqrwym.langora.infrastructure.web.ApiResponse",
            "org.dsqrwym.langora.infrastructure.web.dto.ParamErrorDetail",
            "org.dsqrwym.langora.infrastructure.web.dto.ValidationErrorDetail",
    };

    public static class AppRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
            // Resource hints
            hints.resources()
                    .registerPattern("banner.txt")
                    .registerPattern("db/migration/*");

            // jOOQ reflection hints for built-in data types and array types in GraalVM
            hints.reflection().registerType(org.jooq.Decfloat.class, MemberCategory.values());

            // The array classes jOOQ needs while initialising SQLDataType. Array classes have no
            // members, the category that matters is UNSAFE_ALLOCATED, which is what allows
            // Array.newInstance(..) for a registered array class at run time.
            for (String arrayType : JOOQ_BUILT_IN_ARRAY_TYPES)
                hints.reflection().registerType(TypeReference.of(arrayType), MemberCategory.values());

            // jOOQ instantiates generated records through Class.getDeclaredConstructor() + newInstance().
            for (String recordType : JOOQ_GENERATED_RECORD_TYPES)
                hints.reflection().registerType(
                        TypeReference.of(recordType),
                        MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS
                );

            // Hibernate Validator instantiates these through their (declared) no-arg constructor.
            for (String validatorType : APP_CONSTRAINT_VALIDATORS)
                hints.reflection().registerType(
                        TypeReference.of(validatorType),
                        MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS
                );

            // Jackson reads/writes these; records additionally need their accessor methods registered.
            for (String payloadType : APP_JSON_PAYLOAD_TYPES)
                hints.reflection().registerType(TypeReference.of(payloadType), MemberCategory.values());

            // Only registered so that Class.forName(..) succeeds, no member access is required.
            for (String dataTypeClass : JOOQ_DIALECT_DATA_TYPES)
                hints.reflection().registerType(TypeReference.of(dataTypeClass));
        }
    }
}
