package com.example.data.model

data class CollectionPoint(
    val id: String,
    val city: String,
    val department: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val openingHours: String,
    val contactPhone: String,
    val officeUnit: String,
    val instructions: List<String>,
    val acceptedItems: List<String>,
    val verifiedSource: String,
    val isVerified: Boolean = true
)

object ColombianCollectionPoints {
    val points = listOf(
        CollectionPoint(
            id = "col_armenia",
            city = "Armenia",
            department = "Quindío",
            name = "Alcaldía Municipal de Armenia - CAM",
            address = "Carrera 16 #15-28, Centro Administrativo Municipal",
            latitude = 4.5339,
            longitude = -75.6811,
            openingHours = "Lunes a Viernes 8:00 AM - 12:00 PM y 2:00 PM - 5:00 PM",
            contactPhone = "(606) 7417100",
            officeUnit = "Oficina Municipal para la Gestión del Riesgo (OMGERD) / Secretaría de Gobierno",
            instructions = listOf(
                "Ingresar por la entrada principal del CAM y solicitar turno para OMGERD.",
                "Entregar el celular apagado y previamente restablecido a valores de fábrica para salvaguardar tu privacidad.",
                "Indicar si el dispositivo aún enciende o si se dona exclusivamente para aprovechamiento de piezas/sensores.",
                "Recibir tu comprobante digital de donación comunitaria EcoSismo."
            ),
            acceptedItems = listOf("Smartphones Android", "Cargadores y cables USB", "Baterías en buen estado (no infladas)", "Placas lógicas"),
            verifiedSource = "Portal Oficial Alcaldía de Armenia / Directorio Institucional"
        ),
        CollectionPoint(
            id = "col_pereira",
            city = "Pereira",
            department = "Risaralda",
            name = "Alcaldía de Pereira - Palacio Municipal",
            address = "Carrera 7 #18-55, Plaza de Bolívar",
            latitude = 4.8143,
            longitude = -75.6946,
            openingHours = "Lunes a Viernes 7:30 AM - 4:00 PM (Jornada Continua)",
            contactPhone = "(606) 3248000",
            officeUnit = "Dirección de Gestión del Riesgo (DIGER) / Secretaría de Gobierno",
            instructions = listOf(
                "Acercarse al punto de atención al ciudadano en el primer piso del Palacio Municipal.",
                "Depositar en el contenedor seguro de la Red Comunitaria de Sensores.",
                "El equipo técnico evaluará en sitio la operatividad del acelerómetro.",
                "Se entrega constancia de aporte a la economía circular comunitaria."
            ),
            acceptedItems = listOf("Smartphones con pantalla intacta o partida", "Baterías funcionales", "Cables microUSB/Tipo C"),
            verifiedSource = "Directorio de Trámites y Sedes - Alcaldía de Pereira"
        ),
        CollectionPoint(
            id = "col_cali",
            city = "Cali",
            department = "Valle del Cauca",
            name = "Alcaldía de Santiago de Cali - CAM",
            address = "Avenida 2 Norte #10-70, Centro Administrativo Municipal",
            latitude = 3.4516,
            longitude = -76.5320,
            openingHours = "Lunes a Viernes 8:00 AM - 5:00 PM",
            contactPhone = "(602) 8879020",
            officeUnit = "Secretaría de Gestión del Riesgo de Emergencias y Desastres",
            instructions = listOf(
                "Dirigirse a la Torre Alcaldía, piso 3 - Gestión del Riesgo.",
                "Completar en la app el formulario de entrega antes o durante la recepción.",
                "Asegurarse de retirar la tarjeta SIM y memoria MicroSD del teléfono.",
                "Los celulares son reacondicionados para la red de monitoreo en laderas de Siloé y Terrón Colorado."
            ),
            acceptedItems = listOf("Celulares 3G/4G con WiFi funcional", "Sensores inerciales operativos", "Fuentes de poder"),
            verifiedSource = "Página Oficial Alcaldía de Cali / Sedes Administrativas"
        ),
        CollectionPoint(
            id = "col_medellin",
            city = "Medellín",
            department = "Antioquia",
            name = "Alcaldía de Medellín - La Alpujarra",
            address = "Calle 44 #52-165, Centro Administrativo La Alpujarra",
            latitude = 6.2442,
            longitude = -75.5736,
            openingHours = "Lunes a Jueves 7:30 AM - 5:30 PM, Viernes 7:30 AM - 4:30 PM",
            contactPhone = "(604) 3855555",
            officeUnit = "DAGRD (Departamento Administrativo de Gestión del Riesgo de Desastres)",
            instructions = listOf(
                "Presentarse en el sótano A o taquillas de atención ciudadana de La Alpujarra.",
                "Registrar el modelo del celular y su estado con el funcionario enlace.",
                "Dispositivos son calibrados e integrados a estaciones comunitarias en comunas vulnerables.",
                "Validación de acelerómetro y giroscopio en el laboratorio comunitario."
            ),
            acceptedItems = listOf("Smartphones Android 7+", "Sensores de movimiento operativos", "Cargadores 5V/1A"),
            verifiedSource = "Portal Medellín.gov.co / Sedes Ciudadanas"
        ),
        CollectionPoint(
            id = "col_ibague",
            city = "Ibagué",
            department = "Tolima",
            name = "Alcaldía Municipal de Ibagué",
            address = "Calle 9 #2-59, Plaza de Bolívar",
            latitude = 4.4389,
            longitude = -75.2322,
            openingHours = "Lunes a Jueves 7:30 AM - 4:30 PM, Viernes 7:30 AM - 3:30 PM",
            contactPhone = "(608) 2611660",
            officeUnit = "Secretaría de Ambiente y Gestión del Riesgo",
            instructions = listOf(
                "Acceder por el vestíbulo principal hacia la ventanilla única de Gestión del Riesgo.",
                "Entregar los terminales en desuso debidamente rotulados o con el código de la app.",
                "El personal emitirá el certificado de contribución ecológica y prevención comunitaria."
            ),
            acceptedItems = listOf("Celulares en desuso", "Baterías recargables", "Accesorios y cargadores"),
            verifiedSource = "Portal Alcaldía de Ibagué / Directorio Municipal"
        ),
        CollectionPoint(
            id = "col_manizales",
            city = "Manizales",
            department = "Caldas",
            name = "Alcaldía de Manizales - CAM",
            address = "Calle 19 #21-44, Centro Administrativo Municipal",
            latitude = 5.0689,
            longitude = -75.5174,
            openingHours = "Lunes a Jueves 7:00 AM - 12:00 PM y 1:30 PM - 5:00 PM, Viernes 7:00 AM - 3:00 PM",
            contactPhone = "(606) 8879700",
            officeUnit = "Unidad de Gestión del Riesgo (UGR) / Secretaría de Gobierno",
            instructions = listOf(
                "Ubicarse en el módulo UGR en la sede central de la Alcaldía de Manizales.",
                "Revisión preliminar de compatibilidad para sensor de vibración.",
                "Los equipos viables se instalan en laderas y taludes con riesgo de inestabilidad geológica."
            ),
            acceptedItems = listOf("Smartphones con acelerómetro funcional", "Adaptadores de pared", "Powerbanks"),
            verifiedSource = "Página Oficial Alcaldía de Manizales"
        ),
        CollectionPoint(
            id = "col_bogota",
            city = "Bogotá",
            department = "Cundinamarca",
            name = "Alcaldía Mayor de Bogotá - Palacio Liévano",
            address = "Carrera 8 #10-65, Plaza de Bolívar",
            latitude = 4.5981,
            longitude = -74.0758,
            openingHours = "Lunes a Viernes 7:00 AM - 4:30 PM",
            contactPhone = "(601) 3813000",
            officeUnit = "IDIGER (Instituto Distrital de Gestión de Riesgos y Cambio Climático) / Secretaría General",
            instructions = listOf(
                "Presentarse en el punto de atención a la ciudadanía del Palacio Liévano o sede IDIGER.",
                "Indicar participación en el Reto 2 'Sensores al Servicio de la Comunidad'.",
                "Firmar entrega y verificar borrado de credenciales de usuario.",
                "Equipos no funcionales son clasificados para reciclaje responsable de RAEE."
            ),
            acceptedItems = listOf("Teléfonos inteligentes Android", "Baterías de ion de litio en buen estado", "Accesorios"),
            verifiedSource = "Bogota.gov.co / Guía de Trámites y Puntos de Atención"
        ),
        CollectionPoint(
            id = "col_bucaramanga",
            city = "Bucaramanga",
            department = "Santander",
            name = "Alcaldía de Bucaramanga - Fase I y II",
            address = "Calle 35 #10-43, Paseo del Comercio",
            latitude = 7.1193,
            longitude = -73.1227,
            openingHours = "Lunes a Jueves 7:30 AM - 12:00 PM y 1:00 PM - 5:00 PM, Viernes 7:00 AM - 4:00 PM",
            contactPhone = "(607) 6337000",
            officeUnit = "Subsecretaría de Gestión del Riesgo y Seguridad",
            instructions = listOf(
                "Ingresar a la Fase II, ventanilla de recepción técnica.",
                "Reportar la marca, modelo y antigüedad aproximada del dispositivo móvil.",
                "Destinados a la red de alerta temprana de Santander (zona sísmica de alta actividad Mesa de los Santos)."
            ),
            acceptedItems = listOf("Dispositivos móviles con acelerómetro/giroscopio", "Cables de carga continua"),
            verifiedSource = "Bucaramanga.gov.co / Información Institucional"
        ),
        CollectionPoint(
            id = "col_quibdo",
            city = "Quibdó",
            department = "Chocó",
            name = "Alcaldía Municipal de Quibdó",
            address = "Carrera 2 #24-A-32, Centro Administrativo",
            latitude = 5.6922,
            longitude = -76.6582,
            openingHours = "Lunes a Viernes 8:00 AM - 12:00 PM y 2:00 PM - 6:00 PM",
            contactPhone = "(604) 6711424",
            officeUnit = "Coordinación Municipal de Gestión del Riesgo de Desastres",
            instructions = listOf(
                "Acercarse al despacho de Gestión del Riesgo en el primer nivel.",
                "Entregar el teléfono para el monitoreo de inestabilidad ribereña y movimientos en masa.",
                "Se validará la capacidad de conexión WiFi o 3G del equipo para telemetría continua."
            ),
            acceptedItems = listOf("Celulares con conectividad inalámbrica", "Antenas y cargadores solares/básicos"),
            verifiedSource = "Alcaldía de Quibdó - Chocó / Sede Electrónica Oficial"
        ),
        CollectionPoint(
            id = "col_neiva",
            city = "Neiva",
            department = "Huila",
            name = "Alcaldía de Neiva - Edificio Municipal",
            address = "Carrera 5 #9-74, Parque Santander",
            latitude = 2.9273,
            longitude = -75.2819,
            openingHours = "Lunes a Jueves 7:00 AM - 12:00 PM y 2:00 PM - 6:00 PM, Viernes 7:00 AM - 12:00 PM y 2:00 PM - 5:00 PM",
            contactPhone = "(608) 8711100",
            officeUnit = "Oficina de Gestión del Riesgo de Desastres (OGRD)",
            instructions = listOf(
                "Presentarse en la ventanilla OGRD en el Centro de Atención al Ciudadano.",
                "Hacer entrega del teléfono celular para la red de monitoreo geológico del Alto Magdalena.",
                "Recibirás código QR de seguimiento de tu equipo dentro de la red comunitaria."
            ),
            acceptedItems = listOf("Smartphones Android", "Adaptadores de corriente", "Cables USB"),
            verifiedSource = "Alcaldía de Neiva / Directorio de Dependencias Municipales"
        )
    )
}
