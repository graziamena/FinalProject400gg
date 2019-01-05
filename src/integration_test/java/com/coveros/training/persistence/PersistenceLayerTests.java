package com.coveros.training.persistence;

import com.coveros.training.domainobjects.Book;
import com.coveros.training.domainobjects.Borrower;
import com.coveros.training.persistence.PersistenceLayer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static com.coveros.training.Constants.DATABASE_URL;
import static com.coveros.training.TestConstants.PATH_TO_PG_RESTORE;
import static com.coveros.training.TestConstants.RESTORE_SCRIPTS_PATH;
import static com.coveros.training.database_backup_constants.*;

/**
 * Test that we have a persistence layer that we can easily mock out.
 * This exists so we can have more control over the persistence process,
 * whether we want to mock those sections, and so on.
 */
public class PersistenceLayerTests {

    private Connection createConnection() {
        Properties props = new Properties();
        props.setProperty("user","postgres");
        props.setProperty("password","postgres");
        Connection conn;
        try {
            conn = DriverManager.getConnection(DATABASE_URL, props);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return conn;
    }

    /**
     * assert that there is a way to store a borrower
     * in a database.  We don't actually care how this happens,
     * we just care that it exists.  Here we're adding a
     * brand-spanking-new borrower.
     */
    @Test
    public void testShouldSaveBorrowerToDatabase() {
        setDatabaseState(INITIAL_STATE_V2_DUMP);
        PersistenceLayer pl = new PersistenceLayer();

        long id = pl.saveNewBorrower("alice");

        Assert.assertEquals(1, id);
    }

    /**
     * We ought to be able to update a borrower's details,
     * if we know that borrower's id and we have a detail we
     * want to change.
     */
    @Test
    public void testShouldUupdateBorrowerToDatabase() {
        setDatabaseState(ONE_PERSON_IN_BORROWER_TABLE_V2_DUMP);
        PersistenceLayer pl = new PersistenceLayer();

        pl.updateBorrower(1, "bob");

        String name = pl.getBorrowerName(1);
        Assert.assertEquals("bob", name);
    }

    /**
     * If a borrower is in the database, we should be able
     * to find that person by their name
     */
    @Test
    public void testShouldBeAbleToSearchBorrowerByName() {
        setDatabaseState(ONE_PERSON_IN_BORROWER_TABLE_V2_DUMP);
        PersistenceLayer pl = new PersistenceLayer();

        Borrower bd = pl.searchBorrowerDataByName("alice");

        Assert.assertEquals("alice", bd.name);
        Assert.assertEquals(1, bd.id);
    }

    /**
     * If a book is in the database, we should be able to find it by title.
     */
    @Test
    public void testShouldBeAbleToSearchForBooksByTitle() {
        setDatabaseState(ONE_BOOK_IN_DB_V2_DUMP);
        PersistenceLayer pl = new PersistenceLayer();

        Book book = pl.searchBooksByTitle("Hitchhiker's Guide to the Galaxy");

        Assert.assertEquals("Hitchhiker's Guide to the Galaxy", book.title);
        Assert.assertEquals(1, book.id);
    }

    /**
     * A helper in the test process - put in any restore script name
     * to get the database into that state.
     */
    @Test
    public void setState() {
        setDatabaseState(INITIAL_STATE_V2_DUMP);
    }

    /**
     * use a "restore" command to set the database into a state
     * of empty tables.  This operation must happen very quickly.
     * To dump a database, run this command:
     * pg_dump -Fc training > <DESCRIPTION>_<VERSION>.dump
     * place it in the db_sample_files directory.
     */
    public static void setDatabaseState(String restoreScriptName) {
        Runtime r = Runtime.getRuntime();
        Process p;
        String restoreScriptPath = Paths.get(RESTORE_SCRIPTS_PATH , restoreScriptName).toString();
        String[] cmd = {
                PATH_TO_PG_RESTORE,
                "--host", "localhost",
                "--port", "5432",
                "--username", "postgres",
                "--dbname", "training",
                "--role", "postgres",
                "--no-password",
                "--clean",  // necessary to enable running again and again without problems.
                restoreScriptPath
        };
        try {
            checkThatFileExists(restoreScriptPath);
            p = r.exec(cmd);
            // following command is necessary to cause the system to wait until the command is done.
            p.waitFor();
        } catch (Exception e) {
            // stop the world if this breaks, and fix it.
            throw new RuntimeException(e);
        }
    }

    private static void checkThatFileExists(String restoreScriptPath) {
        File tmpDir = new File(restoreScriptPath);
        boolean exists = tmpDir.exists();
        if (! exists) {
            throw new RuntimeException("the path to the script was incorrect: " + restoreScriptPath);
        }
    }


}
