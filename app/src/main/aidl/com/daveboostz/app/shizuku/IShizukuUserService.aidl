package com.daveboostz.app.shizuku;

interface IShizukuUserService {
    void destroy() = 16777114;
    String executeAllowlisted(String actionId, String targetPackage) = 1;
    String readSurfaceFlingerStats(String layerHint) = 2;
}
