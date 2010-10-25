import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import edu.stanford.junction.props2.runtime.*;
 
public class PropsLispTest {
 
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
    public void testInstantiate() {
		Lisp lisp = new Lisp();
    }

    @Test
    public void testReadNumber() {
		try{
			Lisp.LispObject o = Lisp.read(new StringReader("5"));
			assertTrue(o instanceof Lisp.LispNumber);
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }
 
}