package com.jmjproductdev.phyter.core.permissions

import io.reactivex.Observable


enum class Permission {
  ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION
}


interface PermissionsManager {
  fun granted(permission: Permission): Boolean
  fun request(vararg permissions: Permission): Observable<Pair<Permission, Boolean>>
}