@param String columns
@param String table
@param String parameters
@param String onConflict

INSERT INTO ${table}
    (${columns})
VALUES
    (${parameters})
${onConflict}
