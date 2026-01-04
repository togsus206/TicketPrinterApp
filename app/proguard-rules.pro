# 1. Mantener los "Genéricos" (Vital para que Gson sepa que es una lista de Tickets)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# 2. NO tocar nada de tu código (Fuerza bruta para evitar errores de nombres)
-keep class com.mval.ticketprinter.** { *; }

# 3. NO tocar nada de Gson
-keep class com.google.gson.** { *; }
-keep class com.google.inject.** { *; }
-dontwarn com.google.gson.**
