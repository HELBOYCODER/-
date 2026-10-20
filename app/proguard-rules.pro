# Keep the JS bridge used by the WebView lock
-keepclassmembers class ir.ersaz.sandogh.LockActivity$Bridge {
    public *;
}
-keepattributes JavascriptInterface
