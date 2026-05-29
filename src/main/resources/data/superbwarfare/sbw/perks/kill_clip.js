// KILL_CLIP: 击杀后换弹获得伤害加成
function modifyProperty(pmc, level, perkTag) {
    if (perkTag && perkTag.getInt("KillClipTime") > 0) {
        pmc.mul("Damage", 1.2 + 0.05 * level)
    }
}

function tick(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.reduceCooldown("KillClipReloadTime")
        perkTag.reduceCooldown("KillClipTime")
    }
}

function preReload(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    var time = perkTag.getInt("KillClipReloadTime")
    if (time > 0) {
        perkTag.remove("KillClipReloadTime")
        perkTag.putBoolean("KillClip", true)
    } else {
        perkTag.remove("KillClip")
    }
}

function postReload(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    if (!perkTag.getBoolean("KillClip")) return
    perkTag.putInt("KillClipTime", 90 + 10 * level)
}

function onKill(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (sourceProxy.isGunDamage()) {
        if (level != 0) {
            perkTag.putInt("KillClipReloadTime", 80)
        }
    }
}
