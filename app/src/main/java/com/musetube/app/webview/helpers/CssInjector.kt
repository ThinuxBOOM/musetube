package com.musetube.app.webview.helpers

import org.json.JSONObject

fun buildCustomCssJs(css: String): String {
    val jsonCss = JSONObject.quote(css)
    return """
        (function(){
            var cst = document.getElementById('musetube-custom-css');
            if ($jsonCss === "") {
                if (cst) cst.remove();
                return;
            }
            if (!cst) {
                cst = document.createElement('style');
                cst.id = 'musetube-custom-css';
            }
            cst.textContent = $jsonCss;
            var target = document.head || document.documentElement;
            if (target && !cst.parentNode) {
                target.appendChild(cst);
            }
        })();
    """.trimIndent()
}
