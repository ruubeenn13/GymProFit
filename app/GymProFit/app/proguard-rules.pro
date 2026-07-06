# ============================================================
# Reglas ProGuard/R8 de GymProFit (release con minifyEnabled + shrinkResources).
# El riesgo principal es Gson: deserializa las respuestas JSON por REFLEXIÓN sobre
# los POJOs de es.pmdm.gymprofit.model.**; si R8 renombra/elimina sus campos, el
# parseo devuelve objetos vacíos/null. Por eso se conservan íntegros.
# Retrofit y OkHttp traen sus propias reglas "consumer" en el AAR (R8 las aplica
# solo); aquí se refuerzan las imprescindibles y se conservan las interfaces de red.
# ============================================================

# ---- Atributos necesarios para genéricos y anotaciones (Gson/Retrofit) ----
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepattributes EnclosingMethod,InnerClasses

# ---- Modelos serializados por Gson: conservar clases, campos y constructores ----
-keep class es.pmdm.gymprofit.model.** { *; }
-keepclassmembers class es.pmdm.gymprofit.model.** { *; }
# Enums dentro de model (Gson usa name()/valueOf por reflexión)
-keepclassmembers enum es.pmdm.gymprofit.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Interfaces Retrofit tipadas y adaptador Gson custom (registrado por reflexión) ----
-keep interface es.pmdm.gymprofit.network.** { *; }
-keep class es.pmdm.gymprofit.network.BooleanNumericAdapter { *; }

# ---- Gson ----
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ---- Retrofit ----
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers,allowshrinking class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ---- OkHttp / Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
