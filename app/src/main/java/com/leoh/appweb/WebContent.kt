package com.leoh.appweb

import android.content.Context
import java.util.*
import java.util.regex.Pattern


object WebContent {
    private val adhosts = mutableListOf<Pattern>()
    private val sitesCfg = mutableMapOf<String, List<String>>()

    fun setup(context: Context) {
        loadAdHosts(context)
        loadSitesConfig(context)
    }

    fun isAdv(url: String): Boolean {
        adhosts.forEach{
            val matcher = it.matcher(url)
            if (matcher.find())
                return true
        }

        return false
    }

    @Synchronized fun getSiteCfg(url: String): List<String>? = sitesCfg.get(url)

    private fun loadAdHosts(context: Context) {
        context.assets.open("adhosts").bufferedReader().forEachLine {
            it.replace(".", "\\.")
                    .replace("?", "\\?")
                    .replace("*", ".*")
                    .apply { adhosts.add(Pattern.compile(it)) }
        }
    }

    private fun loadSitesConfig(context: Context) {
        val lines = context.assets.open("sites.cfg").bufferedReader().readLines()

        val result = lines.map{ it.trim() }
                .filterNot { it.isEmpty() }
                .filterNot { commentedOut(it) }

        var list: MutableList<String>? = null
        result.forEach{
            when (it[0]) {
                '[' ->  {
                    list = mutableListOf<String>();
                    sitesCfg.put(it.substring(1,it.length-1), list!!)
                }
                else -> {
                    val scan = Scanner(it)
                    val tag = scan.next()
                    val attr = scan.next()
                    val text = StringBuilder()

                    if (!tag.startsWith("*"))
                        text.append(tag)
                    if (!attr.startsWith("*"))
                        text.append("[").append(attr).append("]")

                    list?.add(text.toString())
                }
            }
        }
    }

    private fun commentedOut(line: String): Boolean = line.startsWith("#") || line.startsWith(";")
}