// package com.github.hjyang.rag.wms_rag_service.service;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Service;

// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class InventoryQueryService {

//     private final JdbcTemplate jdbcTemplate;

//     // 전체 재고 현황 (최근 수정순 TOP 20)
//     public String getInventorySummary() {
//         String sql = """
//                 SELECT TOP 20
//                     i.INV_ID,
//                     i.LOC_ID,
//                     i.ITEM_ID,
//                     i.ITEM_QTY,
//                     i.UPDATED_AT,
//                     i.UPDATED_BY
//                 FROM IMS.dbo.T_DATA_INVENTORY i
//                 ORDER BY i.UPDATED_AT DESC
//                 """;

//         List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
//         return formatRows(rows);
//     }

//     // 특정 아이템 재고 조회
//     public String getInventoryByItemId(Long itemId) {
//         String sql = """
//                 SELECT
//                     i.INV_ID,
//                     i.LOC_ID,
//                     i.ITEM_ID,
//                     i.ITEM_QTY,
//                     i.UPDATED_AT
//                 FROM IMS.dbo.T_DATA_INVENTORY i
//                 WHERE i.ITEM_ID = ?
//                 """;

//         List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, itemId);
//         return formatRows(rows);
//     }

//     // 수량 기준 상위 재고
//     public String getTopInventory(int limit) {
//         String sql = """
//                 SELECT TOP (?)
//                     i.INV_ID,
//                     i.LOC_ID,
//                     i.ITEM_ID,
//                     i.ITEM_QTY,
//                     i.UPDATED_AT
//                 FROM IMS.dbo.T_DATA_INVENTORY i
//                 ORDER BY i.ITEM_QTY DESC
//                 """;

//         List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit);
//         return formatRows(rows);
//     }

//     // 특정 로케이션 재고 조회
//     public String getInventoryByLocation(Long locId) {
//         String sql = """
//                 SELECT
//                     i.INV_ID,
//                     i.LOC_ID,
//                     i.ITEM_ID,
//                     i.ITEM_QTY,
//                     i.UPDATED_AT
//                 FROM IMS.dbo.T_DATA_INVENTORY i
//                 WHERE i.LOC_ID = ?
//                 """;

//         List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, locId);
//         return formatRows(rows);
//     }

//     // 전체 재고 요약 통계
//     public String getInventoryStats() {
//         String sql = """
//                 SELECT
//                     COUNT(*)        AS 총재고건수,
//                     SUM(ITEM_QTY)   AS 총수량,
//                     MAX(ITEM_QTY)   AS 최대수량,
//                     MIN(ITEM_QTY)   AS 최소수량,
//                     AVG(ITEM_QTY)   AS 평균수량
//                 FROM IMS.dbo.T_DATA_INVENTORY
//                 """;

//         List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
//         return formatRows(rows);
//     }

//     // Map 리스트를 자연어 텍스트로 변환
//     private String formatRows(List<Map<String, Object>> rows) {
//         if (rows.isEmpty()) return "조회된 데이터가 없습니다.";

//         return rows.stream()
//                 .map(row -> row.entrySet().stream()
//                         .map(e -> e.getKey() + "=" + e.getValue())
//                         .collect(Collectors.joining(", ")))
//                 .collect(Collectors.joining("\n"));
//     }
// }