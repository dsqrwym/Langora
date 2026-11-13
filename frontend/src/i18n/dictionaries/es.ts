import type {Dictionary} from "./zh";

const es: Dictionary = {
    common: {
        loading: "Cargando",
        errors: {
            network: "No se puede conectar. Comprueba tu conexión e inténtalo de nuevo.",
            timeout: "El servidor ha tardado demasiado en responder. Inténtalo de nuevo.",
            rateLimited: "Demasiados intentos. Inténtalo de nuevo en un momento.",
            accessDenied: "No tienes acceso a esta acción.",
            sessionExpired: "Tu sesión ha caducado. Inicia sesión de nuevo.",
            accountDisabled: "Esta cuenta está desactivada. Contacta con tu administrador.",
            server: "El servicio no está disponible temporalmente. Inténtalo de nuevo en breve.",
            requestFailed: "La operación no se ha completado. Inténtalo de nuevo.",
            invalidResponse: "El servidor ha devuelto datos que no podemos leer. Contacta con tu desarrollador.",
            validationFailed: "La solicitud ha sido rechazada. Contacta con tu desarrollador.",
            unexpected: "Algo ha salido mal. Inténtalo de nuevo.",
            errorCode: "Código de error: {code}",
        },
    },
    auth: {
        login: "Iniciar sesión",
        title: "Inicia sesión en tu cuenta",
        subtitle: "Introduce tu correo o usuario para continuar",
        identifierLabel: "Correo electrónico o usuario",
        identifierPlaceholder: "Introduce tu correo o usuario",
        passwordLabel: "Contraseña",
        passwordPlaceholder: "Introduce tu contraseña",
        forgotPassword: "¿Has olvidado la contraseña?",
        submitting: "Iniciando sesión…",
        captchaRequired: "Completa la verificación de abajo",
        captchaFailed: "La verificación ha fallado. Inténtalo de nuevo.",
        captchaUnavailable: "La verificación no está disponible temporalmente. Inténtalo de nuevo en breve.",
        errors: {
            invalidUsernameCredentials: "Usuario o contraseña incorrectos",
            invalidEmailCredentials: "Correo o contraseña incorrectos",
            identifierRequired: "Introduce tu correo o usuario",
            invalidEmail: "Introduce un correo electrónico válido",
            usernameMinLength: "El usuario debe tener al menos {min} caracteres",
            usernameInvalidCharacters: "El usuario solo puede contener letras, números, puntos, guiones bajos o guiones",
            passwordRequired: "Introduce una contraseña válida",
            passwordTooLong: "La contraseña es demasiado larga (máx. {max} bytes)",
        },
    },
};

export default es;