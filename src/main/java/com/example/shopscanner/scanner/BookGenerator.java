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
    private static final int HEADER_LINES = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BookGenerator() {
    }

    /**
     * Generates a Written Book from scan results.
     */
    public static ItemStack generateBook(String shopName, List<ContainerScanner.ContainerResult> results) {
        List<String> pages = new ArrayList<>();

        // Count total diamonds across all containers
        int totalDiamonds = countTotalDiamonds(results);

        // Title page
        pages.add(buildTitlePage(shopName, results.size(), totalDiamonds));

        // Container pages
        for (ContainerScanner.ContainerResult result : results) {
            if (pages.size() >= MAX_PAGES) {
                pages.set(pages.size() - 1, "...\n\nMore containers\ncould not be shown.");
                break;
            }
            addContainerPages(pages, result);
        }

        // Create book
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(shopName);
        meta.setAuthor("ShopScanner");

        for (String page : pages) {
            meta.addPages(Component.text(page));
        }

        book.setItemMeta(meta);
        return book;
    }

    private static String buildTitlePage(String shopName, int containerCount, int totalDiamonds) {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        String containerWord = containerCount == 1 ? "Container" : "Containers";

        // Keep title page clean and readable
        return """
               ---------------
                 Shop Scan

                 "%s"

               ---------------
               %s

               %d %s
               Diamonds: %d
               ---------------""".formatted(shopName, date, containerCount, containerWord, totalDiamonds);
    }

    private static int countTotalDiamonds(List<ContainerScanner.ContainerResult> results) {
        int total = 0;
        for (ContainerScanner.ContainerResult result : results) {
            if (result.items() != null) {
                total += result.items().getOrDefault(Material.DIAMOND, 0);
            }
        }
        return total;
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
                pages.add(header + "(empty)");
                return;
            }
            case CHUNK_NOT_LOADED -> {
                pages.add(header + "(chunk not loaded)");
                return;
            }
            case OUT_OF_SCAN_RADIUS -> {
                pages.add(header + "(out of scan range)");
                return;
            }
            case NOT_A_CONTAINER -> {
                String noContainerHeader = "@ %d, %d, %d\n---------------\n".formatted(x, y, z);
                pages.add(noContainerHeader + "(no longer exists\n- removed)");
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

            String itemLine = formatItemLine(entry.getKey(), entry.getValue());

            if (linesOnPage >= MAX_LINES_PER_PAGE) {
                pages.add(currentPage.toString());
                if (pages.size() >= MAX_PAGES) {
                    break;
                }

                // Continuation page header
                String contHeader = "%s (cont.)\n@ %d, %d, %d\n---------------\n"
                        .formatted(result.containerTypeName(), x, y, z);
                currentPage = new StringBuilder(contHeader);
                linesOnPage = HEADER_LINES;
                isFirstPage = false;
            }

            currentPage.append(itemLine);
            linesOnPage++;
        }

        if (currentPage.length() > 0 && (isFirstPage || linesOnPage > HEADER_LINES)) {
            pages.add(currentPage.toString());
        }
    }

    private static String buildContainerHeader(String typeName, int x, int y, int z) {
        String type = typeName != null ? typeName : "Container";
        return "%s\n@ %d, %d, %d\n---------------\n".formatted(type, x, y, z);
    }

    private static String formatItemLine(Material material, int amount) {
        String name = ItemNameUtil.formatMaterialName(material);
        // Shorten long names to fit book width (~19 chars total with amount)
        if (name.length() > 14) {
            name = name.substring(0, 12) + "..";
        }
        return name + " x" + amount + "\n";
    }
}
