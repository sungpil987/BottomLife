extends SceneTree

func _initialize() -> void:
    var settings := EditorInterface.get_editor_settings()
    var sdk := OS.get_environment("ANDROID_HOME")
    var java := OS.get_environment("JAVA_HOME")
    var keystore := "/tmp/reborn-debug.keystore"
    settings.set_setting("export/android/android_sdk_path", sdk)
    settings.set_setting("export/android/java_sdk_path", java)
    settings.set_setting("export/android/debug_keystore", keystore)
    settings.set_setting("export/android/debug_keystore_user", "androiddebugkey")
    settings.set_setting("export/android/debug_keystore_pass", "android")
    print("ANDROID SDK: ", settings.get_setting("export/android/android_sdk_path"))
    print("JAVA SDK: ", settings.get_setting("export/android/java_sdk_path"))
    print("DEBUG KEYSTORE EXISTS: ", FileAccess.file_exists(keystore))
    print("ANDROID TEMPLATE EXISTS: ", FileAccess.file_exists(OS.get_data_dir().path_join("export_templates/4.3.stable/android_debug.apk")))
    quit()
