package org.arch.framework.generater.core;

import lombok.Builder;
import lombok.Data;

/**
 * @author lait.zhang@gmail.com
 * @description: TODO
 * @weixin PN15855012581
 * @date :
 */
@Data
@Builder
public final class TableSchema implements NameToField {
    private Integer row;
    private String table;
    private String column;
    private String type;
    private Integer length;
    private Boolean isnull;
    private String defaultValue;
    private Boolean primaryKey;
    private Boolean unique;
    private String forienKey;
    private String comment;

    @Override
    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}