import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;


public class AndroidInstrument {
	
	public static void main(String[] args) {		
		//prefer Android APK files// -src-prec apk
		
		Options.v().set_src_prec(Options.src_prec_apk);
		
		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);
		Options.v().set_android_jars("/home/mike/Android/Sdk/platforms/");
		Options.v().set_soot_classpath("/home/mike/Android/Sdk/platforms/android-26/android.jar:/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar");
		Options.v().set_allow_phantom_refs(false);
		
        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {    		
			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
				final PatchingChain<Unit> units = b.getUnits();
				
				//important to use snapshotIterator here
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					u.apply(new AbstractStmtSwitch() {
						
						public void caseInvokeStmt(InvokeStmt stmt) {
							InvokeExpr invokeExpr = stmt.getInvokeExpr();

					        String className = invokeExpr.getMethod().getDeclaringClass().getName();
					        String methodName = invokeExpr.getMethod().getName();
					        String msg = "";
					        List<Type> params = invokeExpr.getMethod().getParameterTypes();
					        
							if(!invokeExpr.getMethod().isJavaLibraryMethod() 
									&& (className != null && (className.equals("android.support.v7.app.AppCompatActivity") 
										|| className.equals("android.support.v4.content.ContextCompat")
										|| className.equals("android.content.Intent")
										|| className.equals("android.provider.Settings")
										|| className.equals("com.helloworld.mike.helloworld.MainActivity")
										|| className.equals("android.os.Build")
										|| className.equals("android.widget.Toast")
										|| className.equals("com.helloworld.mike.helloworld.ReplyActivity")
										|| className.equals("com.helloworld.mike.helloworld.PermissionPair")
										|| className.equals("android.content.ContextWrapper")
										|| className.equals("android.app.Activity")
										|| className.equals("android.util.Log")
										|| className.equals("com.google.android.gms.common.GoogleApiAvailability")
										|| className.equals("com.google.android.gms.location.LocationServices")
										|| className.startsWith("com.google.android.gms.common.zzf")))
								) 
							{
								Local tmpRef = addTmpRef(b);
								
								Local tmpString = addTmpString(b);
								
								  // insert "tmpRef = java.lang.System.out;" 
						        units.insertBefore(Jimple.v().newAssignStmt( 
						                      tmpRef, Jimple.v().newStaticFieldRef( 
						                      Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), u);      
						        
	
						        //units.insertBefore(Jimple.v().newAssignStmt(tmpString, 
					            //          StringConstant.v(invokeExpr.getMethod().getClass().getPackage().toString())), u);
						    
						        units.insertBefore(Jimple.v().newAssignStmt(tmpString, 
					                      StringConstant.v(constructQualifiedMethodName(className, methodName, msg, params))), u);
					        
						        // insert "tmpRef.println(tmpString);" 
						        SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");                    
						        units.insertBefore(Jimple.v().newInvokeStmt(
						                      Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), tmpString)), u);
								
						        //check that we did not mess up the Jimple
						        b.validate();
							}
						}
						
					});
				}
			}


		}));
		
		soot.Main.main(args);
	}
	
	private static String constructQualifiedMethodName(String className, String methodName, String msg, List<Type> params) {
		StringBuilder buffer = new StringBuilder();
		
		buffer.append("SOOT: ");
		buffer.append(className);
		buffer.append(".");
		buffer.append(methodName);

		buffer.append("(");
		if (params != null) {
			for (int i = 0; i < params.size(); i++) {
				buffer.append(params.get(i).toQuotedString());
				if (i < params.size() - 1)
					buffer.append(",");
			}
		}
		buffer.append(")");
		
		buffer.append(" ");
		buffer.append(msg);

		return buffer.toString().intern();
	}

    private static Local addTmpRef(Body body)
    {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }
    
    private static Local addTmpString(Body body)
    {
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String")); 
        body.getLocals().add(tmpString);
        return tmpString;
    }
}
