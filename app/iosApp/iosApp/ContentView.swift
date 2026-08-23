//
//  ContentView.swift
//  iosApp
//
//  Created by 万圣 on 2026/1/19.
//

import SwiftUI
import launcher

struct ContentView: View {
    var body: some View {
        ComposeVC()
            .ignoresSafeArea(.all)
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeVC: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> some UIViewController {
        App_iosKt.createSampleController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
        
    }
}

#Preview {
    ContentView()
}
