package grails.plugin.jstest

import java.io.FileInputStream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global

class JavaScriptTestRunner {
	Context cx
	Scriptable scope
	
	String output = ""

	JavaScriptTestRunner() {
		Global global = new Global();
		cx = ContextFactory.getGlobal().enterContext();
		global.init(cx);
		cx.setLanguageVersion(Context.VERSION_1_6)
		scope = cx.initStandardObjects(global)
		scope.put("Loader", scope, new Loader(cx, scope))
		cx.setGeneratingSource(true)
		cx.setGeneratingDebug(true)
		cx.setGenerateObserverCount(true)
		cx.setOptimizationLevel(-1);
	}
	
	Exception lastException
	
	Boolean runTest(String javaScriptFile) {
		lastException = null
		output = ""
		try {
			Object result = runJavascriptFile(cx, scope, javaScriptFile);
			output = cx.toString(result)
			if (output != "undefined") println output //not sure what the "undefined" is from, but it's unnecessary I think.
			return true
		}catch(EvaluatorException evalEx) { // aka compiler error
			output += "JavaScript evaluator exception:\n"
			output += evalEx.message + "\n"
			println output
			lastException = evalEx
			return false
		}catch (org.mozilla.javascript.RhinoException all) {
			output += "JavaScript test fail at:\n"
			String[] stack = all.getScriptStackTrace().split('at')
			output += stack[stack.length-1] //this is the relevant line in the test.
			lastException = all
			println output
			return false
        }catch (other) {
            output += "JavaScript test runtime exception:\n"
            output += other.message
            lastException = other
            println output
            return false
        }
	}
	
	def runJavascriptFile(Context cx, Scriptable scope, String fileName) {
        FileReader scriptReader = new FileReader(fileName)
        try {
            return cx.evaluateReader(scope, scriptReader, fileName, 1, null);
        } finally {
            scriptReader.close()
        }
	}
	
	class Loader {
		Context cx
		Scriptable scope
        def load(String fileName) {
            load(fileName, null)
        }
		def load(String fileName, String encoding) {
            if (! new File(fileName).exists()) throw new RuntimeException("File does not exist: $fileName")

            // FileReader broken by design
            FileReader scriptReader = encoding ? new InputStreamReader(new FileInputStream(fileName), encoding) : new FileReader(fileName)
            try {
                cx.evaluateReader(scope, scriptReader, fileName, 1, null);
            } finally {
                scriptReader.close()
            }
		}
        def loadBuiltin(String fileName) {
            // Assume that builtins are always just ASCII
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("grails/plugin/jstest/builtin/$fileName")
            if (! is) throw new RuntimeException("No such builtin script: $fileName")
            
            Reader scriptReader = new InputStreamReader(is, "US-ASCII")
            try {
                cx.evaluateReader(scope, scriptReader, fileName, 1, null)
            } finally {
                scriptReader.close()
            }
        }
		def println(String string) {
			System.out.println(string);
		}
		Loader(Context cx, Scriptable scope) {
			this.cx = cx
			this.scope = scope
		}
	}
}
