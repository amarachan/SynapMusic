/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * Ext.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.synap.music.discord.utils

import com.synap.music.discord.rpc.RpcImage

fun String.toRpcImage(): RpcImage? {
    return if (this.isBlank()) {
        null
    } else if (this.startsWith("attachments")) {
        RpcImage.DiscordImage(this)
    } else {
        RpcImage.ExternalImage(this)
    }
}
