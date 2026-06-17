package com.salah.tracker.data.model

data class PakistanCity(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffset: Double = 5.0
)

object PakistanCities {
    val cities = listOf(
        PakistanCity("Karachi", 24.8607, 67.0011),
        PakistanCity("Lahore", 31.5204, 74.3587),
        PakistanCity("Islamabad", 33.6844, 73.0479),
        PakistanCity("Rawalpindi", 33.5973, 73.0479),
        PakistanCity("Peshawar", 34.0151, 71.5249),
        PakistanCity("Quetta", 30.1798, 66.9750),
        PakistanCity("Faisalabad", 31.4504, 73.1350),
        PakistanCity("Multan", 30.1575, 71.5249),
        PakistanCity("Gujranwala", 32.1877, 74.1945),
        PakistanCity("Sialkot", 32.4945, 74.5229),
        PakistanCity("Bahawalpur", 29.3544, 71.6911),
        PakistanCity("Sargodha", 32.0740, 72.6861),
        PakistanCity("Sukkur", 27.7244, 68.8475),
        PakistanCity("Jhang", 31.2781, 72.3317),
        PakistanCity("Hyderabad", 25.3960, 68.3578),
        PakistanCity("Gujrat", 32.5742, 74.0754),
        PakistanCity("Mardan", 34.1989, 72.0464),
        PakistanCity("Larkana", 27.5590, 68.2097),
        PakistanCity("Sheikhupura", 31.7131, 73.9783),
        PakistanCity("Mirpur Khas", 25.5251, 69.0159),
        PakistanCity("Rahim Yar Khan", 28.4212, 70.2989),
        PakistanCity("Gwadar", 25.1264, 62.3224),
        PakistanCity("Muzaffarabad (AJK)", 34.3700, 73.4711),
        PakistanCity("Gilgit", 35.9208, 74.3089),
        PakistanCity("Dera Ghazi Khan", 30.0489, 70.6389),
        PakistanCity("Abbottabad", 34.1688, 73.2215),
        PakistanCity("Bahawalnagar", 29.9944, 73.2641),
        PakistanCity("Chiniot", 31.7200, 72.9789),
        PakistanCity("Kamoke", 32.0289, 74.2215),
        PakistanCity("Sadiqabad", 28.3115, 70.1317),
        PakistanCity("Kasur", 31.1156, 74.4456),
        PakistanCity("Okara", 30.8100, 73.4500),
        PakistanCity("Mingora (Swat)", 34.7717, 72.3600),
        PakistanCity("Jhelum", 32.9328, 73.7257),
        PakistanCity("Shikarpur", 27.9575, 68.6381),
        PakistanCity("Nawabshah", 26.2483, 68.4097),
        PakistanCity("Mirpur (AJK)", 33.1481, 73.7533)
    )
}
