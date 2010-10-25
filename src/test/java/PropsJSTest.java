import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import edu.stanford.junction.props2.runtime.*;
 
public class PropsJSTest {
 
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
		CTinyJS js = new CTinyJS();
    }

    @Test
    public void testEvalInteger() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("4");
			assert(result != null);
			assert(result.var.isInt());
			assert(result.var.isNumeric());
			assert(result.var.getInt() == 4);
		} catch (CScriptException e) {
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalNumber() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("4.2");
			assert(result != null);
			assert(result.var.isDouble());
			assert(result.var.isNumeric());
			assert(result.var.getDouble() == 4.2);
		} catch (CScriptException e) {
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalString() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("\"Hello World\"");
			assert(result != null);
			assert(result.var.isString());
			assert(result.var.getString().equals("Hello World"));
		} catch (CScriptException e) {
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalAddInts() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("2 + 3");
			assert(result != null);
			assert(result.var.isNumeric());
			assert(result.var.getInt() == 5);
		} catch (CScriptException e) {
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalAddDoubles() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("2.2 + 3.3");
			assert(result != null);
			assert(result.var.isNumeric());
			assert(result.var.getDouble() == 5.5);
		} catch (CScriptException e) {
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalAddStrings() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("\"A\" + \"B\"");
			assert(result != null);
			assert(result.var.isString());
			assert(result.var.getString().equals("AB"));
		} catch (CScriptException e) {
			System.err.println(e.text);
			fail();
		}
    }


    @Test
    public void testEvalFunction() {
		CTinyJS js = new CTinyJS();
		try{
			CScriptVarLink result = js.evaluateComplex("function(){ var dude = 1; dude + 5; return dude;}");
			assert(result.var.isFunction());
		} catch (CScriptException e) {
			System.err.println(e.text);
			fail();
		}
    }
 
}