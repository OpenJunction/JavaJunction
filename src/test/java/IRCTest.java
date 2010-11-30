import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.Junction;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.activity.JunctionExtra;
import edu.stanford.junction.api.activity.ActivityScript;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.irc.IRCSwitchboardConfig;
 
public class IRCTest {
 
    private Collection collection;
 
    @BeforeClass
    public static void oneTimeSetUp() {}
 
    @AfterClass
    public static void oneTimeTearDown() {}
 
    @Before
    public void setUp() {}
 
    @After
    public void tearDown() {}
 
    @Test
    public void testBasic() {
		// Add test code here.
	}
}