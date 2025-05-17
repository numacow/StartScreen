package com.numa.startscreen.fancymenu;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CustomGUIResolver {
    public static void load() {
        resolveCustomGuis();
    }

    public static boolean resolveCustomGuis() {
        Path customguifile = FabricLoader.getInstance().getConfigDir().resolve("fancymenu/custom_gui_screens.txt");
        try {
            if (!Files.exists(customguifile)) {
                createDefaultFile(customguifile);
                return true;
            }

            List<String> lines = Files.readAllLines(customguifile);

            boolean hasWelcome = lines.stream().anyMatch(line -> line.contains(Screens.STARTSCREEN));

            if (!hasWelcome) {
                lines.add(getDefaultWelcomeGui());
                Files.write(customguifile, lines);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createDefaultFile(Path customguifile) throws IOException {
        List<String> defaultContent = List.of(
                "type = custom_gui_screens",
                "",
                "overridden_screens {",
                "}",
                "",
                getDefaultWelcomeGui()
        );
        Files.write(customguifile, defaultContent);
    }


    private static String getDefaultWelcomeGui() {
        return "custom_gui {\n" +
                "  identifier = startscreen_start\n" +
                "  title = \n" +
                "  allow_esc = true\n" +
                "  transparent_world_background = true\n" +
                "  transparent_world_background_overlay = false\n" +
                "  pause_game = true\n" +
                "}";
    }
}
