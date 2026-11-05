extends SceneTree

func _initialize() -> void:
    print("=== BOTTOMLIFE ANDROID EXPORT DIAGNOSTICS ===")
    print("HOME: ", OS.get_environment("HOME"))
    print("SDK: ", OS.get_environment("ANDROID_HOME"))
    print("JAVA: ", OS.get_environment("JAVA_HOME"))
    var settings = EditorInterface.get_editor_settings()
    for key in ["export/android/android_sdk_path", "export/android/java_sdk_path", "export/android/debug_keystore", "export/android/debug_keystore_user"]:
        if settings.has_setting(key):
            print("SETTING ", key, " = ", settings.get_setting(key))
        else:
            print("SETTING MISSING: ", key)
    var template_dir = OS.get_data_dir().path_join("godot/export_templates/4.3.stable")
    print("TEMPLATES: ", template_dir)
    print("DEBUG TEMPLATE EXISTS: ", FileAccess.file_exists(template_dir.path_join("android_debug.apk")))
    print("RELEASE TEMPLATE EXISTS: ", FileAccess.file_exists(template_dir.path_join("android_release.apk")))
    print("KEYSTORE EXISTS: ", FileAccess.file_exists("/tmp/reborn-debug.keystore"))
    var config = ConfigFile.new()
    var error = config.load("res://export_presets.cfg")
    print("PRESET LOAD ERROR CODE: ", error)
    if error == OK:
        print("PRESET NAME: ", config.get_value("preset.0", "name", "<missing>"))
        print("PRESET PLATFORM: ", config.get_value("preset.0", "platform", "<missing>"))
        print("PRESET TARGET SDK: ", config.get_value("preset.0.options", "package/target_sdk", "<missing>"))
        print("PRESET PACKAGE: ", config.get_value("preset.0.options", "package/unique_name", "<missing>"))
    print("=== END DIAGNOSTICS ===")
    quit()
