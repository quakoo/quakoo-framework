package com.quakoo.hbaseFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.incrementer.AbstractColumnMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class StepMaxValueIncrementer implements DataFieldMaxValueIncrementer, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(StepMaxValueIncrementer.class);

    private DataSource dataSource;

    private DataFieldMaxValueIncrementer incrementer;

    private String tableName;

    private String columnName;

    private int step = 10;

    protected int paddingLength = 0;

    @Override
    public void afterPropertiesSet() throws Exception {
        MaxValueIncrementer incrementer = new MaxValueIncrementer();
        incrementer.setIncrementerName(this.tableName);
        incrementer.setColumnName(this.columnName);
        incrementer.setCacheSize(this.step);
        incrementer.setDataSource(this.dataSource);
        incrementer.afterPropertiesSet();

        this.incrementer = incrementer;

    }

    @Override
    public int nextIntValue() throws DataAccessException {
        return (int) nextLongValue();
    }

    @Override
    public long nextLongValue() throws DataAccessException {
        return this.incrementer.nextLongValue();
    }

    @Override
    public String nextStringValue() throws DataAccessException {
        String s = Long.toString(nextLongValue());
        int len = s.length();
        if (len < this.paddingLength) {
            StringBuilder sb = new StringBuilder(this.paddingLength);
            for (int i = 0; i < this.paddingLength - len; i++) {
                sb.append('0');
            }
            sb.append(s);
            s = sb.toString();
        }
        return s;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setStep(int step) {
        this.step = step;
    }

    private static class MaxValueIncrementer extends AbstractColumnMaxValueIncrementer {
        /**
         * The SQL string for retrieving the new sequence value
         */
        private static final String VALUE_SQL = "select last_insert_id()";

        /**
         * The next id to serve
         */
        private long nextId = 0;

        /**
         * The max id to serve
         */
        private long maxId = 0;

        private int preSeq = -1;

        public static final int[] inc_seq = { 2, 3, 5, 7, 11, 13, 17 };

        @Override
        public void afterPropertiesSet() {
            super.afterPropertiesSet();
            for (int seq : inc_seq) {
                if (super.getCacheSize() <= seq + 10) {
                    throw new IllegalArgumentException("Property 'step' is too small");
                }
            }
        }

        /**
         * Default constructor for meta property style usage.
         * 
         * @see #setDataSource
         * @see #setIncrementerName
         * @see #setColumnName
         */
        public MaxValueIncrementer() {
        }

        /**
         * Convenience constructor.
         * 
         * @param dataSource
         *            the DataSource to use
         * @param incrementerName
         *            the name of the sequence/table to use
         * @param columnName
         *            the name of the column in the sequence table to use
         */
        public MaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
            super(dataSource, incrementerName, columnName);
        }

        @Override
        protected synchronized long getNextKey() throws DataAccessException {
            // nextId ++ 涓�洿鍒颁负maxId锛屾墍浠ュ綋nextId澶т簬鎴栫瓑浜巑axId鍚庨渶瑕侀噸鏂板彇寰楁柊鐨刬d
            int seq = (int) (System.nanoTime() % inc_seq.length);
            if (seq == this.preSeq) {
                seq = ((seq + 1) << 2) % inc_seq.length;
                if (seq == this.preSeq) {
                    seq = ((seq + 2) << 2) % inc_seq.length;
                    if (seq == this.preSeq) {
                        System.out.println(seq);
                    }
                }
            }
            this.nextId += inc_seq[seq];
            if (this.maxId <= this.nextId) {
                /*
                 * Need to use straight JDBC code because we need to make sure
                 * that the insert and select are performed on the same
                 * connection (otherwise we can't be sure that last_insert_id()
                 * returned the correct value)
                 */
                Connection con = DataSourceUtils.getConnection(getDataSource());
                Statement stmt = null;
                try {
                    stmt = con.createStatement();
                    DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
                    // Increment the sequence column...
                    String columnName = getColumnName();
                    stmt.executeUpdate("update " + getIncrementerName() + " set " + columnName + " = last_insert_id("
                            + columnName + " + " + getCacheSize() + ")");
                    // Retrieve the new max of the sequence column...
                    ResultSet rs = stmt.executeQuery(VALUE_SQL);
                    try {
                        if (!rs.next()) {
                            throw new DataAccessResourceFailureException(
                                    "last_insert_id() failed after executing an update");
                        }
                        this.maxId = rs.getLong(1);
                    } finally {
                        JdbcUtils.closeResultSet(rs);
                    }
                    this.nextId = this.maxId - getCacheSize() + inc_seq[seq];
                } catch (SQLException ex) {
                    throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
                } finally {
                    JdbcUtils.closeStatement(stmt);
                    DataSourceUtils.releaseConnection(con, getDataSource());
                }
            }
            this.preSeq = seq;
            return this.nextId;
        }
    }
}
