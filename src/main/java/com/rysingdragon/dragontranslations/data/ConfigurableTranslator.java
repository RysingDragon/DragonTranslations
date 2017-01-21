package com.rysingdragon.dragontranslations.data;

import com.rysingdragon.dragontranslations.DragonTranslations;
import com.rysingdragon.dragontranslations.exceptions.InvalidLocaleException;
import com.rysingdragon.dragontranslations.Translator;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

public class ConfigurableTranslator implements Translator {

    private Map<Locale, HoconFile> langFiles;
    private Path langDir;
    private Locale defaultLocale;

    //Create a new instance of ConfigurableTranslations, providing the path of the language directory to use.
    public ConfigurableTranslator(Path langDir, Locale defaultLocale) {
        this.langFiles = new HashMap<>();
        this.langDir = langDir;
        this.defaultLocale = defaultLocale;
        this.addLocale(defaultLocale);
    }

    //Add a Locale and create the necessary file for it if none found.
    public void addLocale(Locale locale) {
        //Locale is not a supported Locale in Minecraft.
        if (!DragonTranslations.getAllMinecraftLocales().keySet().contains(locale)) {
            try {
                throw new InvalidLocaleException("Provided Locale not available in Minecraft");
            } catch (InvalidLocaleException e) {
                e.printStackTrace();
            }
        }
        Path path = this.langDir.resolve(locale.toString() + ".conf");
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        HoconFile langFile = new HoconFile(HoconConfigurationLoader.builder().setPath(path).build());
        langFile.load();
        this.langFiles.put(locale, langFile);
    }

    //Whether a translation key exists for the specified Locale.
    public boolean keyExists(Locale locale, String key) {
        if (langFiles.containsKey(locale)) {
            HoconFile file = langFiles.get(locale);
            return !file.getNode(key.toLowerCase().split("\\.")).isVirtual();
        } else {
            return false;
        }
    }

    //Set a new translation for the specified key, or create if if hadn't existed previously.
    public void addTranslation(Locale locale, String key, Text value) {
        if (langFiles.containsKey(locale)) {
            HoconFile file = langFiles.get(locale);
            String serialized = TextSerializers.FORMATTING_CODE.serialize(value);
            file.getNode(key.toLowerCase().split("\\.")).setValue(serialized);
            file.save();
        }
    }

    //Translate into the selected locale, if none is found, use the default locale. If there's still no translation found, return the key provided
    @Override
    public Text translate(Locale locale, String key) {
        if (keyExists(locale, key)) {
            HoconFile file = langFiles.get(locale);
            return TextSerializers.FORMATTING_CODE.deserialize(file.getNode(key.toLowerCase().split("\\.")).getString());
        } else if (keyExists(getDefaultLocale(), key)){
            HoconFile file = langFiles.get(getDefaultLocale());
            return TextSerializers.FORMATTING_CODE.deserialize(file.getNode(key.toLowerCase().split("\\.")).getString());
        } else {
            return Text.of(key);
        }
    }

    @Override
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }
}
