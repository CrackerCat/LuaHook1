package com.kulipai.luahook1


import HookLib
import LuaDrawableLoader
import LuaHttp
import LuaImport
import LuaJson
import LuaResourceBridge
import LuaSharedPreferences
import Luafile
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luaj.Globals
import org.luaj.LuaValue
import org.luaj.lib.jse.CoerceJavaToLua
import org.luaj.lib.jse.JsePlatform
import org.luckypray.dexkit.DexKitBridge
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.dexkit.DexFinder

//


class MainHook : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
//        init {
//            System.loadLibrary("dexkit")
//        }

        const val MODULE_PACKAGE = "com.kulipai.luahook1"  // 模块包名

    }


    lateinit var luaScript: String

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {

        XpHelper.initZygote(startupParam);
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {

        luaScript = """
            

imports "android.widget.Toast"

hook("android.app.Activity",
lpparam.classLoader,
"onCreate",
"android.os.Bundle",
function(it)

end,
function(it)
  Toast.makeText(it.thisObject,"Luahook1",1000).show()
end)
        """.trimIndent()


        val globals: Globals = JsePlatform.standardGlobals()

        //加载Lua模块
        globals["XpHelper"] = CoerceJavaToLua.coerce(XpHelper::class.java)
        globals["DexFinder"] = CoerceJavaToLua.coerce(DexFinder::class.java)
        globals["XposedHelpers"] = CoerceJavaToLua.coerce(XposedHelpers::class.java)
        globals["XposedBridge"] = CoerceJavaToLua.coerce(XposedBridge::class.java)
        globals["DexKitBridge"] = CoerceJavaToLua.coerce(DexKitBridge::class.java)
        globals["this"] = CoerceJavaToLua.coerce(this)
        HookLib(lpparam).call(globals)
        LuaJson().call(globals)
        LuaHttp().call(globals)
        Luafile().call(globals)
        LuaSharedPreferences().call(globals)
        globals["imports"] = LuaImport(lpparam.classLoader, this::class.java.classLoader!!, globals)

        LuaResourceBridge().registerTo(globals)

        LuaDrawableLoader().registerTo(globals)

        //全局脚本
        try {
            //排除自己
            if (lpparam.packageName != MODULE_PACKAGE) {
                val chunk: LuaValue = globals.load(luaScript)
                chunk.call()
            }
        } catch (e: Exception) {

        }


    }


}