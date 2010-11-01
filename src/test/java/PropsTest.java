import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import org.json.JSONObject;
import edu.stanford.junction.props2.*;
 
public class PropsTest {
 
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
		InputStream is = getClass().getResourceAsStream( "/test.prop" );
		Prop prop = new Prop("dude", new InputStreamReader(is));
    }
}