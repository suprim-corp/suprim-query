package dev.suprim.query.model.context;

import dev.suprim.query.model.DbTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
public class DeleteContext {
    String dbId;
    String tableName;
    DbTable table;
    String where;
    Map<String, Object> paramMap;
    Map<String, Object> data;

    public void createParamMap() {
        if (isNull(paramMap)) {
            paramMap = new HashMap<>();
        }
    }
}
