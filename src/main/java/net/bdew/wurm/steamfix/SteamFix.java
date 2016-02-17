package net.bdew.wurm.steamfix;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.util.logging.Logger;

public class SteamFix implements WurmMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("SteamFix");

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctLoginHandler = classPool.getCtClass("com.wurmonline.server.LoginHandler");
            ctLoginHandler.getMethod("reallyHandle", "(ILjava/nio/ByteBuffer;)V").instrument(new ExprEditor() {
                @Override
                public void edit(Handler h) throws CannotCompileException {
                    h.insertBefore("logger.log(java.util.logging.Level.SEVERE, \"Exception in loginhandler\", $1);");
                    logger.info("Added exception logging at line " + h.getLineNumber() + " in LoginHandler.reallyHandle");
                }

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("BeginAuthSession")) {
                        m.replace("$_ = $proceed($$); logger.log(java.util.logging.Level.INFO, \"Steam Auth \"+$1+\" -> \"+$_);");
                        logger.info("Added auth logging at line " + m.getLineNumber() + " in LoginHandler.reallyHandle");
                    }
                }
            });

            CtClass ctSocketConnection = classPool.getCtClass("com.wurmonline.communication.SocketConnection");
            ctSocketConnection.getMethod("getIp", "()Ljava/lang/String;").setBody("{" +
                    "   if (this.socket != null) {" +
                    "     return this.socket.getInetAddress().toString();" +
                    "   } else {" +
                    "       logger.log(java.util.logging.Level.WARNING, \"Something is trying to get the IP of a dead socket\", (Throwable) new Exception());" +
                    "       return \"<closed socket>\";" +
                    "   }" +
                    "}");

            logger.info("Applied workaround for null socket error");

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preInit() {
    }
}
