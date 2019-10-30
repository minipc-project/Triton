package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.ComponentUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import java.util.*;

public class AdvancedComponent {

    private String text;
    private HashMap<String, String> components = new HashMap<>();
    private HashMap<String, List<AdvancedComponent>> translatableArguments = new HashMap<>();

    public static AdvancedComponent fromBaseComponent(BaseComponent... components) {
        AdvancedComponent advancedComponent = new AdvancedComponent();
        StringBuilder builder = new StringBuilder();
        for (BaseComponent comp : components) {
            boolean hasClick = false;
            boolean hasHover = false;
            if (comp.hasFormatting()) {
                if (comp.getColorRaw() != null)
                    builder.append(comp.getColorRaw().toString());
                if (comp.isBold())
                    builder.append(ChatColor.BOLD.toString());
                if (comp.isItalic())
                    builder.append(ChatColor.ITALIC.toString());
                if (comp.isUnderlined())
                    builder.append(ChatColor.UNDERLINE.toString());
                if (comp.isStrikethrough())
                    builder.append(ChatColor.STRIKETHROUGH.toString());
                if (comp.isObfuscated())
                    builder.append(ChatColor.MAGIC.toString());
                if (comp.getClickEvent() != null && !comp.getClickEvent().getValue()
                        .endsWith("[/" + Triton.get().getConf().getChatSyntax().getLang() + "]")) {
                    builder.append("\uE400");
                    builder.append(ComponentUtils.encodeClickAction(comp.getClickEvent().getAction()));
                    UUID uuid = UUID.randomUUID();
                    advancedComponent.setComponent(uuid, comp.getClickEvent().getValue());
                    builder.append(uuid.toString());
                    hasClick = true;
                }
                if (comp.getHoverEvent() != null) {
                    builder.append("\uE500");
                    builder.append(ComponentUtils.encodeHoverAction(comp.getHoverEvent().getAction()));
                    UUID uuid = UUID.randomUUID();
                    advancedComponent.setComponent(uuid, TextComponent.toLegacyText(comp.getHoverEvent().getValue()));
                    builder.append(uuid.toString());
                    hasHover = true;
                }
            }
            if (comp instanceof TextComponent)
                builder.append(((TextComponent) comp).getText());
            if (comp instanceof TranslatableComponent) {
                TranslatableComponent tc = (TranslatableComponent) comp;
                UUID uuid = UUID.randomUUID();
                builder.append("\uE600")
                        .append(tc.getTranslate())
                        .append("\uE600")
                        .append(uuid)
                        .append("\uE600");
                List<AdvancedComponent> args = new ArrayList<>();
                if (tc.getWith() != null)
                    for (BaseComponent arg : tc.getWith())
                        args.add(fromBaseComponent(arg));
                advancedComponent.setTranslatableArguments(uuid.toString(), args);
            }
            if (comp.getExtra() != null) {
                AdvancedComponent component = fromBaseComponent(comp.getExtra().toArray(new BaseComponent[0]));
                builder.append(component.getText());
                for (Map.Entry<String, String> entry : component.getComponents().entrySet())
                    advancedComponent.setComponent(entry.getKey(), entry.getValue());
                for (Map.Entry<String, List<AdvancedComponent>> entry :
                        component.getAllTranslatableArguments().entrySet())
                    advancedComponent.setTranslatableArguments(entry.getKey(), entry.getValue());
            }
            if (hasHover)
                builder.append("\uE501");
            if (hasClick)
                builder.append("\uE401");

        }
        advancedComponent.setText(builder.toString());
        return advancedComponent;
    }

    public BaseComponent[] toBaseComponent() {
        return new BaseComponent[]{new TextComponent(toBaseComponent(this.text).toArray(new BaseComponent[0]))};
    }

    private List<BaseComponent> toBaseComponent(String text) {
        List<BaseComponent> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        TextComponent component = new TextComponent("");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                i++;
                if (i >= text.length()) {
                    builder.append(c);
                    continue;
                }
                ChatColor format = ChatColor.getByChar(Character.toLowerCase(text.charAt(i)));
                if (format == null) {
                    builder.append(c);
                    i--;
                    continue;
                }
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = component.getColor();
                    list.add(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                }
                switch (format) {
                    case BOLD:
                        component.setBold(true);
                        break;
                    case ITALIC:
                        component.setItalic(true);
                        break;
                    case UNDERLINE:
                        component.setUnderlined(true);
                        break;
                    case STRIKETHROUGH:
                        component.setStrikethrough(true);
                        break;
                    case MAGIC:
                        component.setObfuscated(true);
                        break;
                    case RESET:
                        component.setColor(ChatColor.WHITE);
                        break;
                    default:
                        component.setColor(format);
                        break;
                }
            } else if (c == '\uE400') {
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    BaseComponent previousComponent = component;
                    list.add(component);
                    component = new TextComponent("");
                    component.copyFormatting(previousComponent, ComponentBuilder.FormatRetention.FORMATTING, false);
                }
                ClickEvent.Action action = ComponentUtils
                        .decodeClickAction(Integer.parseInt(Character.toString(text.charAt(i + 1))));
                String uuid = text.substring(i + 2, i + 2 + 36);
                component.setClickEvent(new ClickEvent(action, this.getComponent(uuid)));
                i += 2 + 36;
                int deep = 0;
                StringBuilder content = new StringBuilder();
                while (text.charAt(i) != '\uE401' || deep != 0) {
                    char c1 = text.charAt(i);
                    if (c1 == '\uE400') deep++;
                    if (c1 == '\uE401') deep--;
                    content.append(c1);
                    i++;
                }
                component.setExtra(toBaseComponent(content.toString()));
                BaseComponent previousComponent = component;
                list.add(component);
                component = new TextComponent("");
                component.copyFormatting(previousComponent, ComponentBuilder.FormatRetention.FORMATTING, false);
            } else if (c == '\uE500') {
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    BaseComponent previousComponent = component;
                    list.add(component);
                    component = new TextComponent("");
                    component.copyFormatting(previousComponent, ComponentBuilder.FormatRetention.FORMATTING, false);
                }
                HoverEvent.Action action = ComponentUtils
                        .decodeHoverAction(Integer.parseInt(Character.toString(text.charAt(i + 1))));
                String uuid = text.substring(i + 2, i + 2 + 36);
                component.setHoverEvent(new HoverEvent(action, TextComponent.fromLegacyText(this.getComponent(uuid))));
                i += 2 + 36;
                int deep = 0;
                StringBuilder content = new StringBuilder();
                while (text.charAt(i) != '\uE501' || deep != 0) {
                    char c1 = text.charAt(i);
                    if (c1 == '\uE500') deep++;
                    if (c1 == '\uE501') deep--;
                    content.append(c1);
                    i++;
                }
                component.setExtra(toBaseComponent(content.toString()));
                BaseComponent previousComponent = component;
                list.add(component);
                component = new TextComponent("");
                component.copyFormatting(previousComponent, ComponentBuilder.FormatRetention.FORMATTING, false);
            } else if (c == '\uE600') {
                i++;
                StringBuilder key = new StringBuilder();
                while (text.charAt(i) != '\uE600') {
                    key.append(text.charAt(i));
                    i++;
                }
                i++;
                StringBuilder uuid = new StringBuilder();
                while (text.charAt(i) != '\uE600') {
                    uuid.append(text.charAt(i));
                    i++;
                }
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    BaseComponent previousComponent = component;
                    list.add(component);
                    component = new TextComponent("");
                    component.copyFormatting(previousComponent, ComponentBuilder.FormatRetention.FORMATTING, false);
                }
                TranslatableComponent tc = new TranslatableComponent(key.toString());
                tc.copyFormatting(component, ComponentBuilder.FormatRetention.FORMATTING, false);
                List<AdvancedComponent> argsAdvanced = this.getTranslatableArguments(uuid.toString());
                if (argsAdvanced != null)
                    for (AdvancedComponent ac : argsAdvanced) {
                        BaseComponent[] bc = ac.toBaseComponent();
                        tc.addWith(bc == null ? new TextComponent("") : bc[0]);
                    }
                list.add(tc);
            } else
                builder.append(c);
        }
        if (builder.length() != 0) {
            component.setText(builder.toString());
            list.add(component);
        }
        return list;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setComponent(UUID uuid, String text) {
        components.put(uuid.toString(), text);
    }

    public void setComponent(String uuid, String text) {
        components.put(uuid, text);
    }

    public String getComponent(String uuid) {
        return components.get(uuid);
    }

    public void setTranslatableArguments(String uuid, List<AdvancedComponent> list) {
        translatableArguments.put(uuid, list);
    }

    public List<AdvancedComponent> getTranslatableArguments(String uuid) {
        return translatableArguments.get(uuid);
    }

    public HashMap<String, List<AdvancedComponent>> getAllTranslatableArguments() {
        return translatableArguments;
    }

    public HashMap<String, String> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "AdvancedComponent{" +
                "text='" + text + '\'' +
                ", components=" + components +
                ", translatableArguments=" + translatableArguments +
                '}';
    }
}