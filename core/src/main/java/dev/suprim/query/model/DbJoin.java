package dev.suprim.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbJoin {
    private String tableName;
    private String alias;
    private String joinType;

    private DbColumn onLeft;
    private DbColumn onRight;
    private String onOperator;

    private List<DbJoinAndCondition> andConditions;
    private List<String> additionalWhere;

    public String render() {
        StringBuilder str = new StringBuilder(
                joinType + " JOIN " + tableName + " " + alias + "\n");

        if (nonNull(onLeft)) {
            str.append(" ON ").append(onLeft.render()).append(" ")
               .append(onOperator).append(" ").append(onRight.render());
        }

        if (nonNull(andConditions) && !andConditions.isEmpty()) {
            for (DbJoinAndCondition cond : andConditions) {
                str.append("\n AND ")
                   .append(cond.leftColumn.render())
                   .append(" ")
                   .append(cond.operator)
                   .append(" ")
                   .append(cond.rightColumn.render());
            }
        }

        if (nonNull(additionalWhere) && !additionalWhere.isEmpty()) {
            for (String where : additionalWhere) {
                str.append("\n AND ").append(where);
            }
        }

        return str + " \n ";
    }

    public void addOn(DbColumn leftColumn, String operator, DbColumn rightColumn) {
        this.onRight = rightColumn;
        this.onLeft = leftColumn;
        this.onOperator = operator;
    }

    public void addAndCondition(DbColumn leftColumn, String operator, DbColumn rightColumn) {
        if (isNull(andConditions)) {
            andConditions = new ArrayList<>();
        }
        andConditions.add(new DbJoinAndCondition(leftColumn, operator, rightColumn));
    }

    public void addAdditionalWhere(String where) {
        if (isNull(additionalWhere)) {
            additionalWhere = new ArrayList<>();
        }
        additionalWhere.add(where);
    }

    private record DbJoinAndCondition(DbColumn leftColumn, String operator, DbColumn rightColumn) {}
}
