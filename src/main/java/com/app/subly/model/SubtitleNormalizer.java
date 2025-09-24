package com.app.subly.model;

import java.util.ArrayList;
import java.util.List;

public final class SubtitleNormalizer {

    private SubtitleNormalizer() {
    }

    public static void normalizeList(List<Subtitle> list) {
        if (list == null) return;
        // Copy to avoid concurrent modification
        List<Subtitle> work = new ArrayList<>(list);

        // Trim & null-safe
        for (Subtitle s : work) {
            s.setPrimaryText(safe(s.getPrimaryText()));
            s.setSecondaryText(safe(s.getSecondaryText()));
        }

        // Remove trailing blank rows
        while (!work.isEmpty()) {
            Subtitle last = work.get(work.size() - 1);
            if (isBlank(last)) {
                work.remove(work.size() - 1);
            } else break;
        }

        ensureAtLeastOne(work);
        renumber(work);

        list.clear();
        list.addAll(work);
    }

    public static void ensureAtLeastOne(List<Subtitle> list) {
        if (list.isEmpty()) {
            list.add(new Subtitle(1, "", ""));
        }
    }

    private static boolean isBlank(Subtitle s) {
        return safe(s.getPrimaryText()).isEmpty() && safe(s.getSecondaryText()).isEmpty();
    }

    private static void renumber(List<Subtitle> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setId(i + 1);
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}