import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import org.json.JSONObject;
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
			LispObject o = (new Lisp()).read(new PushbackReader(new StringReader("5")));
			assertTrue(o instanceof LispNumber);
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }


    @Test
    public void testReadList() {
		try{
			LispObject o = (new Lisp()).read(new PushbackReader(new StringReader("(1 2 3)")));
			assertTrue(o instanceof LispCons);
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testReadNestedList() {
		try{
			LispObject o = (new Lisp()).read(new PushbackReader(new StringReader("(aemon (2 (horse \"dude\" face 1)))")));
			assertTrue(o instanceof LispCons);
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalNumber() {
		try{
			LispObject o = (new Lisp()).eval(new PushbackReader(new StringReader("2323")));
			assertTrue(o instanceof LispNumber);
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalSpecial() {
		try{
			LispObject o = (new Lisp()).eval(
				new PushbackReader(
					new StringReader("(let ((a 69)) a)")));
			assertTrue(o instanceof LispNumber);
			assertTrue(((LispNumber)o).value.equals(new Integer(69)));
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalIf() {
		try{
			LispObject o = (new Lisp()).eval(
				new PushbackReader(
					new StringReader("(let ((a t)) (if a 55 22))")));
			assertTrue(o instanceof LispNumber);
			assertTrue(((LispNumber)o).value.equals(new Integer(55)));
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testEvalFn() {
		try{
			LispObject o = (new Lisp()).eval(
				new PushbackReader(
					new StringReader("((fn (a) a) 23)")));
			assertTrue(o instanceof LispNumber);
			assertTrue(((LispNumber)o).value.equals(new Integer(23)));
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }

    @Test
    public void testJSONFuncs() {
		try{
			Lisp lisp = new Lisp();
			LispObject f = lisp.eval(
				new PushbackReader(
					new StringReader("(fn (obj) (put obj \"name\" \"Joe\"))")));
			assertTrue(f instanceof LispFunc);
			JSONObject obj = new JSONObject();
			((LispFunc)f).apply(new LispCons(new LispJSONObject(obj), Lisp.nil), lisp);
			assertTrue(obj.optString("name").equals("Joe"));
		}
		catch(Exception e){
			e.printStackTrace(System.err);
			fail();
		}
    }
 
}