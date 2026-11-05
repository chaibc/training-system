package com.hanmo.calligraphy.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装，与前端约定的统一分页结构。
 *
 * <p>records 为当前页数据，total 为总记录数，page / size 回显请求的页码与每页条数。</p>
 */
@Data
@AllArgsConstructor
public class PageResult<T> {
    // 当前页数据
    private List<T> records;
    // 总记录数
    private long total;
    // 当前页码（从 1 开始）
    private long page;
    // 每页条数
    private long size;

    /**
     * 构造分页结果。
     *
     * @param records 当前页数据
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页条数
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }
}
