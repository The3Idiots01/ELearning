package com.learnova.elearning.module.review.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lớp 1: Bộ lọc từ cấm Offline tốc độ cao sử dụng thuật toán Aho-Corasick kết hợp chuẩn hóa văn bản.
 * Độ phức tạp: O(N) theo độ dài nhận xét.
 */
@Service
@Slf4j
public class ProfanityFilterService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://|www\\.|t\\.me/|zalo\\.me/|bit\\.ly/)[a-zA-Z0-9+&@#/%?=~_|!:,.;]*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(0|\\+84)[3|5|7|8|9][0-9]{8}|(0|\\+84)[2][0-9]{9}"
    );

    private final AhoCorasickTrie trie;

    public ProfanityFilterService() {
        this.trie = new AhoCorasickTrie();
        initKeywords();
    }

    private void initKeywords() {
        List<String> keywords = List.of(
                // Từ ngữ thô tục, chửi thề tiếng Việt phổ biến & viết tắt
                "đm", "dm", "dmm", "đmm", "dcm", "đcm", "clmm", "vcl", "vcc", "vkl",
                "địt", "dit", "dit me", "địt mẹ", "đụ", "du", "đụ má", "du ma", "đụ mẹ", "du me",
                "lồn", "lon", "con lồn", "cái lồn", "mặt lồn", "xàm lồn", "hãm lồn", "ngu lồn",
                "cặc", "cac", "con cặc", "thằng cặc", "ăn cặc", "buồi", "buoi", "con buồi",
                "dái", "dai", "bìu", "chó chết", "cho chet", "óc chó", "oc cho", "đồ chó",
                "đĩ", "di", "con đĩ", "đĩ mẹ", "điếm", "diem", "phò", "pho", "con phò",
                "cút", "cut", "biến đi", "ngu như chó", "ngu như bò",

                // Nội dung tình dục, gạ gẫm, quấy rối 18+
                "chat sex", "chatsex", "gạ tình", "ga tinh", "gái gọi", "gai goi", "gái bao", "gai bao",
                "cave", "massage a-z", "massage từ a-z", "massage a z", "massage az",
                "show hàng", "show hang", "sextoy", "sex toy", "clip sex", "clip 18+", "video sex",
                "phim sex", "phim heo", "nude", "khỏa thân", "khoa than", "lộ clip", "lo clip",
                "thủ dâm", "thu dam", "dâm đãng", "dam dang", "kích dục", "kich duc",
                "nhìn ngon", "nhin ngon", "nhìn ngon quá", "nhìn ngon thế", "body ngon",
                "múp rụp", "mup rup", "muốn húp", "muon hup", "húp vội", "hup voi", "nứng", "nung",

                // Tiếng Anh thô tục
                "fuck", "fucking", "bitch", "asshole", "shit", "motherfucker", "bastard", "dick", "pussy", "cunt",

                // Cờ bạc, lừa đảo, chất cấm
                "tài xỉu", "tai xiu", "cá độ", "ca do", "kubet", "shbet", "789club", "sunwin", "baccarat",
                "đánh bạc", "danh bac", "kẹo ke", "keo ke", "mai thúy", "bay lắc", "bay lac"
        );

        for (String kw : keywords) {
            trie.insert(normalize(kw));
        }
        trie.buildFailureLinks();
        log.info("Initialized ProfanityFilterService with {} banned keywords in Aho-Corasick automaton", keywords.size());
    }

    public ModerationCheckResult check(String text) {
        if (text == null || text.isBlank()) {
            return ModerationCheckResult.pass();
        }

        // 1. Kiểm tra URL spam
        if (URL_PATTERN.matcher(text).find()) {
            return ModerationCheckResult.fail("Nội dung chứa đường dẫn liên kết ngoài (URL) không được phép.");
        }

        // 2. Kiểm tra số điện thoại (chống spam gạ gẫm/zalo)
        String compactText = text.replaceAll("[\\s.-]", "");
        if (PHONE_PATTERN.matcher(compactText).find()) {
            return ModerationCheckResult.fail("Nội dung chứa số điện thoại hoặc thông tin liên lạc ngoài.");
        }

        // 3. Chuẩn hóa chuỗi và kiểm tra bằng Aho-Corasick
        String normalized = normalize(text);
        List<String> matched = trie.search(normalized);
        if (!matched.isEmpty()) {
            return ModerationCheckResult.fail("Nội dung chứa từ ngữ thô tục hoặc không phù hợp (" + matched.get(0) + ").");
        }

        // 4. Kiểm tra chuỗi không dấu (chống lách bỏ dấu)
        String stripped = stripAccents(normalized);
        List<String> matchedStripped = trie.search(stripped);
        if (!matchedStripped.isEmpty()) {
            return ModerationCheckResult.fail("Nội dung chứa từ ngữ thô tục hoặc không phù hợp (" + matchedStripped.get(0) + ").");
        }

        return ModerationCheckResult.pass();
    }

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(Locale.ROOT);
        // Thay thế ký tự leetspeak phổ biến
        s = s.replace('@', 'a')
                .replace('0', 'o')
                .replace('3', 'e')
                .replace('1', 'i')
                .replace('!', 'i')
                .replace('$', 's')
                .replace('|', 'i');

        // Bỏ ký tự phân cách xen kẽ (d.m, d-m, d_m, d*m => dm)
        s = s.replaceAll("([a-z0-9])[*._\\-~]([a-z0-9])", "$1$2");
        return s;
    }

    public static String stripAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.replace('đ', 'd').replace('Đ', 'd');
    }

    public record ModerationCheckResult(boolean allowed, String reason) {
        public static ModerationCheckResult pass() {
            return new ModerationCheckResult(true, null);
        }

        public static ModerationCheckResult fail(String reason) {
            return new ModerationCheckResult(false, reason);
        }
    }

    /**
     * Cài đặt bộ tự động Aho-Corasick thuần túy O(N).
     */
    private static class AhoCorasickTrie {
        private final Node root = new Node();

        private static class Node {
            final Map<Character, Node> children = new HashMap<>();
            final List<String> outputs = new ArrayList<>();
            Node fail;
        }

        public void insert(String word) {
            if (word == null || word.isEmpty()) return;
            Node curr = root;
            for (char ch : word.toCharArray()) {
                curr = curr.children.computeIfAbsent(ch, c -> new Node());
            }
            curr.outputs.add(word);
        }

        public void buildFailureLinks() {
            Queue<Node> queue = new LinkedList<>();
            for (Node child : root.children.values()) {
                child.fail = root;
                queue.add(child);
            }

            while (!queue.isEmpty()) {
                Node current = queue.poll();

                for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
                    char ch = entry.getKey();
                    Node child = entry.getValue();

                    Node fallback = current.fail;
                    while (fallback != null && !fallback.children.containsKey(ch)) {
                        fallback = fallback.fail;
                    }

                    child.fail = (fallback == null) ? root : fallback.children.get(ch);
                    if (child.fail != null) {
                        child.outputs.addAll(child.fail.outputs);
                    }
                    queue.add(child);
                }
            }
        }

        public List<String> search(String text) {
            List<String> results = new ArrayList<>();
            Node curr = root;

            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);

                while (curr != null && !curr.children.containsKey(ch)) {
                    curr = curr.fail;
                }

                if (curr == null) {
                    curr = root;
                    continue;
                }

                curr = curr.children.get(ch);
                if (curr != null && !curr.outputs.isEmpty()) {
                    // Kiểm tra word boundary đối với các từ ngắn (<= 3 ký tự) để tránh false positive
                    for (String out : curr.outputs) {
                        if (out.length() <= 3) {
                            int start = i - out.length() + 1;
                            int end = i;
                            boolean prevCharBoundary = (start == 0) || !Character.isLetterOrDigit(text.charAt(start - 1));
                            boolean nextCharBoundary = (end == text.length() - 1) || !Character.isLetterOrDigit(text.charAt(end + 1));
                            if (prevCharBoundary && nextCharBoundary) {
                                results.add(out);
                            }
                        } else {
                            results.add(out);
                        }
                    }
                }
            }

            return results;
        }
    }
}
