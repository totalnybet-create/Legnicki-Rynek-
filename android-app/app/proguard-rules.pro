# Zachowaj metadane wymagane przez Retrofit, Kotlin Serialization i adnotacje DI.
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# Klasy generowane przez Kotlin Serialization nie używają refleksji.
# Reguły konsumenckie Room, Hilt, Retrofit, OkHttp i Coil są dostarczane
# przez ich biblioteki. Nie wyłączamy optymalizacji globalnie.

-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
