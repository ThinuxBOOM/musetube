package com.musetube.app.webview.helpers

fun buildAmoledJs(enabled: Boolean): String {
    return if (enabled) {
        """
            (function(){
                var aled = document.getElementById('musetube-amoled-theme') || document.createElement('style');
                aled.id = 'musetube-amoled-theme';
                aled.textContent = 'ytmusic-app{background:#000!important} ytmusic-player-bar{background:#000!important;box-shadow:none;border-top:1px solid #666} ytmusic-nav-bar{background:#000!important}';
                if (!aled.parentNode) (document.head || document.documentElement).appendChild(aled);
            })();
        """.trimIndent()
    } else {
        """
            (function(){
                var aled = document.getElementById('musetube-amoled-theme');
                if (aled) aled.remove();
            })();
        """.trimIndent()
    }
}
