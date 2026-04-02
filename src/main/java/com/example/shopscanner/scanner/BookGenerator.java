package com.example.shopscanner.scanner;

import com.example.shopscanner.utils.ItemNameUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generates Written Books from scan results.
 */
public final class BookGenerator {

    private static final int MAX_PAGES = 100;
    private static final int MAX_LINES_PER_PAGE = 14;
    private static final int HEADER_LINES = 3; // Container type + coords + separator
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private BookGenerator() {
    }

    /**
     * Generates a Written Book from scan results.
     *
     * @param shopName the shop name
     * @param results  the scan results
     * @return the Written Book ItemStack
     */
    public static ItemStack generateBook(String shopName, List<ContainerScanner.ContainerResult> results) {
        List<String> pages = new ArrayList<>();

        // Title page
        pages.add(buildTitlePage(shopName, results.size()));

        // Container pages
        for (ContainerScanner.ContainerResult result : results) {
            if (pages.size() >= MAX_PAGES) {
                // Replace last page with overflow message
                pages.set(pages.size() - 1, "... weitere Container\nnicht darstellbar");
                break;
            }
            addContainerPages(pages, result);
        }

        // Create book
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(shopName);
        meta.setAuthor("ShopScanner");

        // Use Adventure Components for pages
        for (String page : pages) {
            meta.addPages(Component.text(page));
        }

        book.setItemMeta(meta);
        return book;
    }

    private static String buildTitlePage(String shopName, int containerCount) {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        return """
                ══════════════
                  Shop Scan
                  "%s"
                ══════════════
                %s

                %d Container
                ══════════════""".formatted(shopName, date, containerCount);
    }

    private static void addContainerPages(List<String> pages, ContainerScanner.ContainerResult result) {
        if (pages.size() >= MAX_PAGES) {
            return;
        }

        int x = result.location().getBlockX();
        int y = result.location().getBlockY();
        int z = result.location().getBlockZ();

        String header = buildContainerHeader(result.containerTypeName(), x, y, z);

        // Handle special statuses
        switch (result.status()) {
            case EMPTY -> {
                pages.add(header + "(leer)");
                return;
            }
            case CHUNK_NOT_LOADED -> {
                pages.add(header + "(Chunk nicht geladen)");
                return;
            }
            case OUT_OF_SCAN_RADIUS -> {
                pages.add(header + "(außerhalb Scan-Radius)");
                return;
            }
            case NOT_A_CONTAINER -> {
                String noContainerHeader = "@ %d, %d, %d\n──────────────\n".formatted(x, y, z);
                pages.add(noContainerHeader + "(kein Container mehr\n - wurde entfernt)");
                return;
            }
            case OK -> {
                // Continue with item listing
            }
        }

        // Sort items by amount descending
        List<Map.Entry<Material, Integer>> sortedItems = result.items().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        // Build pages with items
        StringBuilder currentPage = new StringBuilder(header);
        int linesOnPage = HEADER_LINES;
        boolean isFirstPage = true;

        for (Map.Entry<Material, Integer> entry : sortedItems) {
            if (pages.size() >= MAX_PAGES) {
                break;
            }

            String itemLine = ItemNameUtil.formatMaterialName(entry.getKey()) + " x " + entry.getValue() + "\n";

            if (linesOnPage >= MAX_LINES_PER_PAGE) {
                // Save current page and start new one
                pages.add(currentPage.toString());
                if (pages.size() >= MAX_PAGES) {
                    break;
                }

                // Continuation page
                String contHeader = result.containerTypeName() + " (Forts.)\n@ %d, %d, %d\n──────────────\n"
                        .formatted(x, y, z);
                currentPage = new StringBuilder(contHeader);
                linesOnPage = HEADER_LINES;
                isFirstPage = false;
            }

            currentPage.append(itemLine);
            linesOnPage++;
        }

        // Add final page if it has content
        if (currentPage.length() > 0 && (isFirstPage || linesOnPage > HEADER_LINES)) {
            pages.add(currentPage.toString());
        }
    }

    private static String buildContainerHeader(String typeName, int x, int y, int z) {
        String type = typeName != null ? typeName : "Container";
        return "%s\n@ %d, %d, %d\n──────────────\n".formatted(type, x, y, z);
    }
}
