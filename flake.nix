{
  description = "Dev shell for developing Java-GI (GTK4 / GNOME) applications";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";  # Need PR 515614 for libadwaita, see https://nixpk.gs/pr-tracker.html?pr=515614
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Native libraries Java-GI binds to. The Java FFM API loads these
        # at runtime via dlopen, so they must be on LD_LIBRARY_PATH.
        nativeLibs = with pkgs; [
          glib
          gobject-introspection
          gtk4
#          libadwaita             # Doesn't compile on macOS
          gdk-pixbuf
          pango
          cairo
          harfbuzz
          graphene
#          gst_all_1.gstreamer      # remove if you don't use GStreamer bindings
          gst_all_1.gst-plugins-base
#          libsoup_3                # remove if you don't use Soup/WebKit
          gsettings-desktop-schemas
        ];
      in
      {

        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            jdk25_headless
            (pkgs.gradle_9-unwrapped.override { java = pkgs.jdk25_headless; })
            # (pkgs.maven.override { jdk_headless = pkgs.jdk25_headless; })
          ] ++ nativeLibs;

          # Let the FFM linker find the GTK/GLib shared objects
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath nativeLibs;
          DYLD_LIBRARY_PATH = pkgs.lib.makeLibraryPath nativeLibs;  # for macOS/Darwin

          shellHook = ''
            # GSettings schemas + typelibs so GTK apps actually run
            export XDG_DATA_DIRS="$GSETTINGS_SCHEMAS_PATH:${pkgs.gtk4}/share/gsettings-schemas/${pkgs.gtk4.name}:$XDG_DATA_DIRS"
            export GI_TYPELIB_PATH="${pkgs.lib.makeSearchPath "lib/girepository-1.0" nativeLibs}"

            # Silence/enable native access warnings for FFM downcalls
            export JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"

            echo "Java-GI dev shell ready — $(java -version 2>&1 | head -n1)"
          '';
        };
      });
}

