package com.gestioncaravana.adapter.out.rules;

import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.domain.CaravanFeatType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownCaravanFeatCatalogAdapter implements CaravanFeatCatalogPort {

  private static final Pattern LEVEL_PATTERN = Pattern.compile("(?i)\\bNivel\\s+(\\d+)\\b");
  private static final Pattern BULLET_PATTERN = Pattern.compile("^\\- \\*\\*(.+?):\\*\\*\\s*(.+)$");
  private static final Pattern REPEATABLE_PATTERN =
      Pattern.compile("(?i)\\b(varias veces|más de una vez|se apilan|se acumulan)\\b");
  private static final Pattern EXPLICIT_LIMIT_PATTERN =
      Pattern.compile("(?i)\\bhasta\\s+(\\d+)\\s+veces\\b");
  private static final Pattern WORD_LIMIT_PATTERN =
      Pattern.compile("(?i)\\b(?:una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez)\\s+veces\\b");
  private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|(.+)\\|$");
  private static final Pattern ESCAPED_PIPE_PATTERN = Pattern.compile("\\\\\\|");
  private final List<CaravanFeatType> feats;

  public MarkdownCaravanFeatCatalogAdapter() {
    this.feats = load();
  }

  @Override
  public List<CaravanFeatType> all() {
    return List.copyOf(feats);
  }

  @Override
  public Optional<CaravanFeatType> findByCode(String code) {
    return feats.stream().filter(feat -> feat.code().equals(code)).findFirst();
  }

  private List<CaravanFeatType> load() {
    var rulesPath = Path.of("docs", "Reglas_de_Caravana.md");
    var automationPath = Path.of("openspec", "specs", "caravan-feat-automation", "spec.md");
    try {
      var content = Files.readString(rulesPath, StandardCharsets.UTF_8);
      var automationContent = Files.readString(automationPath, StandardCharsets.UTF_8);
      var automationByCode = parseAutomationSpec(automationContent);
      return parse(content, automationByCode);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to load feat catalog from " + rulesPath.toAbsolutePath() + " and " + automationPath.toAbsolutePath(),
          e);
    }
  }

  private List<CaravanFeatType> parse(String content, Map<String, AutomationRow> automationByCode) {
    var dotesIndex = content.indexOf("## Dotes");
    if (dotesIndex < 0) {
      throw new IllegalStateException("Dotes section not found in rules document");
    }
    var tail = content.substring(dotesIndex);
    var lines = tail.split("\\R");
    var result = new ArrayList<CaravanFeatType>();

    String currentName = null;
    List<String> blockLines = new ArrayList<>();

    for (var line : lines) {
      if (line.startsWith("## ") && !line.startsWith("### ") && !line.startsWith("#### ") && !line.equals("## Dotes")) {
        break;
      }
      if (line.startsWith("#### ")) {
        if (currentName != null) {
          result.add(parseBlock(currentName, blockLines, automationByCode));
        }
        currentName = line.substring(5).trim();
        blockLines = new ArrayList<>();
      } else if (currentName != null) {
        blockLines.add(line);
      }
    }

    if (currentName != null) {
      result.add(parseBlock(currentName, blockLines, automationByCode));
    }

    return result;
  }

  private CaravanFeatType parseBlock(String name, List<String> lines, Map<String, AutomationRow> automationByCode) {
    var joined = String.join("\n", lines).trim();
    var paragraphs = joined.split("(\\R\\s*\\R)+");
    var description = paragraphs.length > 0 ? normalizeText(paragraphs[0]) : null;

    var prerequisites = new ArrayList<String>();
    var benefit = nullString();
    var special = nullString();
    var notes = nullString();

    for (var line : lines) {
      var trimmed = line.trim();
      var matcher = BULLET_PATTERN.matcher(trimmed);
      if (!matcher.matches()) {
        continue;
      }
      var label = matcher.group(1).trim().toLowerCase();
      var value = normalizeText(matcher.group(2).trim());
      switch (label) {
        case "requisito", "requisitos", "prerrequisito", "prerrequisitos" -> prerequisites.add(value);
        case "beneficio" -> benefit = value;
        case "especial" -> special = value;
        case "nota" -> notes = value;
        default -> {
        }
      }
    }

    if (benefit == null) {
      benefit = description == null ? name : description;
    }

    var repeatable = isRepeatable(joined);
    var selectionLimit = parseSelectionLimit(joined, repeatable);
    var minimumLevel = parseMinimumLevel(prerequisites, joined);
    var automation = automationByCode.get(toCode(name));

    return new CaravanFeatType(
        toCode(name),
        name,
        description == null ? name : description,
        prerequisites,
        benefit,
        special,
        notes,
        repeatable,
        selectionLimit,
        minimumLevel,
        automation == null ? null : automation.mode(),
        automation == null ? null : automation.stateInputs(),
        automation == null ? null : automation.exactAutomation());
  }

  private Map<String, AutomationRow> parseAutomationSpec(String content) {
    var specIndex = content.indexOf("## 10. Feat-by-Feat Implementation Contract");
    if (specIndex < 0) {
      throw new IllegalStateException("Feat-by-feat implementation contract section not found in automation spec");
    }
    var tail = content.substring(specIndex);
    var lines = tail.split("\\R");
    var result = new HashMap<String, AutomationRow>();
    var inTable = false;

    for (var line : lines) {
      if (!inTable) {
        if (line.startsWith("| Feat | Mode | State / Inputs | Exact automation |")) {
          inTable = true;
        }
        continue;
      }
      if (!line.startsWith("|")) {
        break;
      }
      if (line.startsWith("|---")) {
        continue;
      }

      var columns = splitMarkdownTableRow(line);
      if (columns.size() < 4) {
        continue;
      }

      var featName = normalizeText(columns.get(0));
      var mode = normalizeText(columns.get(1));
      var stateInputs = normalizeText(columns.get(2));
      var exactAutomation = normalizeText(columns.get(3));
      if (featName == null || mode == null || stateInputs == null || exactAutomation == null) {
        continue;
      }

      result.put(toCode(featName), new AutomationRow(mode, stateInputs, unescapePipes(exactAutomation)));
    }

    return result;
  }

  private List<String> splitMarkdownTableRow(String line) {
    var matcher = TABLE_ROW_PATTERN.matcher(line.trim());
    if (!matcher.matches()) {
      return List.of();
    }

    var raw = matcher.group(1);
    var columns = new ArrayList<String>();
    var current = new StringBuilder();
    var escaped = false;
    for (var i = 0; i < raw.length(); i++) {
      var ch = raw.charAt(i);
      if (escaped) {
        current.append(ch);
        escaped = false;
        continue;
      }
      if (ch == '\\') {
        escaped = true;
        continue;
      }
      if (ch == '|') {
        columns.add(current.toString().trim());
        current.setLength(0);
        continue;
      }
      current.append(ch);
    }
    columns.add(current.toString().trim());
    return columns;
  }

  private String unescapePipes(String value) {
    return ESCAPED_PIPE_PATTERN.matcher(value).replaceAll("|");
  }

  private String normalizeText(String text) {
    if (text == null) {
      return null;
    }
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed.replaceAll("\\s+", " ");
  }

  private boolean isRepeatable(String text) {
    if (REPEATABLE_PATTERN.matcher(text).find()) {
      return true;
    }

    var lower = text.toLowerCase();
    return lower.contains("puede elegirse varias veces")
        || lower.contains("puede seleccionarse varias veces")
        || lower.contains("puede seleccionarse más de una vez")
        || lower.contains("se puede adquirir tres veces")
        || lower.contains("se puede adquirir más de una vez")
        || lower.contains("puede adquirirse varias veces")
        || lower.contains("puede elegirse esta dote varias veces")
        || lower.contains("puede seleccionarse esta dote varias veces")
        || lower.contains("puede adquirirse esta dote varias veces")
        || lower.contains("puede seleccionarse hasta")
        || lower.contains("puede adquirirse hasta")
        || lower.contains("se puede adquirir hasta")
        || lower.contains("se puede seleccionar hasta");
  }

  private int parseSelectionLimit(String text, boolean repeatable) {
    if (!repeatable) {
      return 1;
    }

    var explicitMatcher = EXPLICIT_LIMIT_PATTERN.matcher(text);
    if (explicitMatcher.find()) {
      return Integer.parseInt(explicitMatcher.group(1));
    }

    var wordMatcher = WORD_LIMIT_PATTERN.matcher(text);
    if (wordMatcher.find()) {
      var fragment = wordMatcher.group().toLowerCase();
      if (fragment.contains("dos")) {
        return 2;
      }
      if (fragment.contains("tres")) {
        return 3;
      }
      if (fragment.contains("cuatro")) {
        return 4;
      }
      if (fragment.contains("cinco")) {
        return 5;
      }
      if (fragment.contains("seis")) {
        return 6;
      }
      if (fragment.contains("siete")) {
        return 7;
      }
      if (fragment.contains("ocho")) {
        return 8;
      }
      if (fragment.contains("nueve")) {
        return 9;
      }
      if (fragment.contains("diez")) {
        return 10;
      }
    }
    return 999;
  }

  private Integer parseMinimumLevel(List<String> prerequisites, String fullText) {
    for (var prerequisite : prerequisites) {
      var matcher = LEVEL_PATTERN.matcher(prerequisite);
      if (matcher.find()) {
        return Integer.valueOf(matcher.group(1));
      }
    }
    var matcher = LEVEL_PATTERN.matcher(fullText);
    if (matcher.find()) {
      return Integer.valueOf(matcher.group(1));
    }
    return null;
  }

  private String nullString() {
    return null;
  }

  private String toCode(String name) {
    var normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return normalized.toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
  }

  private record AutomationRow(String mode, String stateInputs, String exactAutomation) {}
}
