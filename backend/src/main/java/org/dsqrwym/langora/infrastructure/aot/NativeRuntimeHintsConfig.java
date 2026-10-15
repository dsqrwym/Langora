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

            // Only registered so that Class.forName(..) succeeds, no member access is required.
            for (String dataTypeClass : JOOQ_DIALECT_DATA_TYPES)
                hints.reflection().registerType(TypeReference.of(dataTypeClass));
        }
    }
}
