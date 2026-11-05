@tool
extends SceneTree

func _initialize() -> void:
    print("=== BOTTOMLIFE ANDROID EXPORT DIAGNOSTICS ===")
    print("HOME: ", OS.get_environment("HOME"))
    print("SDK: ", OS.get_environment("ANDROID_HOME"))
    print("JAVA: ", OS.get_environment("JAVA_HOME"))
    var settings := EditorInterface.get_editor_settings()
    for key in ["export/android/android_sdk_path", "export/android/java_sdk_path", "export/android/debug_keystore", "export/android/debug_keystore_user"]:
        print("SETTING ", key, " = ", settings.get_setting(key) if settings.has_setting(key) else "<MISSING>")
    var templates := OS.get_data_dir().path_join("export_templates/4.3.stable")
    print("TEMPLATES: ", templates)
    print("DEBUG TEMPLATE EXISTS: ", FileAccess.file_exists(templates.path_join("android_debug.apk")))
    print("KEYSTORE EXISTS: ", FileAccess.file_exists("/tmp/reborn-debug.keystore"))
    var count := EditorExport.get_export_preset_count()
    print("EXPORT PRESET COUNT: ", count)
    for i in range(count):
        var preset := EditorExport.get_export_preset(i)
        print("PRESET ", i, " = ", preset.get_name(), " / ", preset.get_platform().get_name())
        var platform := preset.get_platform()
        var err := ""
        var valid := platform.can_export(preset, true, err)
        print("CAN EXPORT DEBUG: ", valid)
        print("EXPORT CONFIG ERRORS: ", err)
    print("=== END DIAGNOSTICS ===")
    quit()
