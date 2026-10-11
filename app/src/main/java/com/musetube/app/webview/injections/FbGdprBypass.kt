package com.musetube.app.webview.injections

object FbGdprBypass {
    const val CONTENT = """
            (function(){
                var btn = document.querySelector('#facebook div[role=button]');
                if(btn) btn.click();
            })();
        
    """
}
