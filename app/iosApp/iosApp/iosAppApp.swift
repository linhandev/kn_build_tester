//
//  iosAppApp.swift
//  iosApp
//
//  Created by 万圣 on 2026/1/19.
//

import SwiftUI
import DanceUI
import launcher
import MyShims

@main
struct iosAppApp: App {
    
    init() {
        my_pre_main()
        InitKmpSpiKt.doInitKmpSpi(host: nil)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
