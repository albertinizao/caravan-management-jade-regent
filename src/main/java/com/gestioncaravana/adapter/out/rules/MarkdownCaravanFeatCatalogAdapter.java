package com.gestioncaravana.adapter.out.rules;

import com.gestioncaravana.application.port.out.CaravanFeatCatalogPort;
import com.gestioncaravana.domain.CaravanFeatType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownCaravanFeatCatalogAdapter implements CaravanFeatCatalogPort {

  private static final Pattern LEVEL_PATTERN = Pattern.compile("(?i)\\bNivel\\s+(\\d+)\\b");
  private static final Pattern TRES_VECES_PATTERN = Pattern.compile("(?i)\\btres veces\\b");
  private static final Pattern SELECTION_LIMIT_PATTERN = Pattern.compile("(?i)\\b(?:puede|se puede|pueden|se pueden)\\s+(?:elegirse|seleccionarse|adquirir)\\s+([^\\.]+)");
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
    try {
      var content = Files.readString(rulesPath, StandardCharsets.UTF_8);
      return parse(content);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load feat catalog from " + rulesPath.toAbsolutePath(), e);
    }
  }

  private List<CaravanFeatType> parse(String content) {
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
          result.add(parseBlock(currentName, blockLines));
        }
        currentName = line.substring(5).trim();
        blockLines = new ArrayList<>();
      } else if (currentName != null) {
        blockLines.add(line);
      }
    }

    if (currentName != null) {
      result.add(parseBlock(currentName, blockLines));
    }

    return result;
  }

  private CaravanFeatType parseBlock(String name, List<String> lines) {
    var joined = String.join("\n", lines).trim();
    var paragraphs = joined.split("(\\R\\s*\\R)+");
    var description = paragraphs.length > 0 ? normalizeText(paragraphs[0]) : null;

    var prerequisites = new ArrayList<String>();
    var benefit = nullString();
    var special = nullString();
    var notes = nullString();

    for (var line : lines) {
      var trimmed = line.trim();
      if (!trimmed.startsWith("- **")) {
        continue;
      }
      var labelEnd = trimmed.indexOf("**:", 4);
      if (labelEnd < 0) {
        continue;
      }
      var label = trimmed.substring(4, labelEnd).trim().toLowerCase();
      var value = normalizeText(trimmed.substring(labelEnd + 3).trim());
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

    return new CaravanFeatType(
        toCode(name),
        name,
        prerequisites,
        benefit,
        special,
        notes,
        repeatable,
        selectionLimit,
        minimumLevel);
  }

  private String normalizeText(String text) {
    if (text == null) {
      return null;
    }
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed.replaceAll("\\s+", " ");
  }

  private boolean isRepeatable(String text) {
    var lower = text.toLowerCase();
    return lower.contains("puede elegirse varias veces")
        || lower.contains("puede seleccionarse varias veces")
        || lower.contains("puede seleccionarse más de una vez")
        || lower.contains("se apilan")
        || lower.contains("se puede adquirir tres veces")
        || lower.contains("se puede adquirir más de una vez")
        || lower.contains("puede adquirirse varias veces");
  }

  private int parseSelectionLimit(String text, boolean repeatable) {
    if (!repeatable) {
      return 1;
    }
    if (TRES_VECES_PATTERN.matcher(text).find()) {
      return 3;
    }
    var matcher = SELECTION_LIMIT_PATTERN.matcher(text);
    if (matcher.find()) {
      var fragment = matcher.group(1).toLowerCase();
      if (fragment.contains("tres")) {
        return 3;
      }
      if (fragment.contains("dos")) {
        return 2;
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
}
