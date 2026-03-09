# Keep the JavascriptInterface for NwCompat and others
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the methods in NwCompat that are called via reflection in asyncCall
-keepclassmembers class com.bread.isat.NwCompat {
    public java.lang.String *(org.json.JSONObject);
}
