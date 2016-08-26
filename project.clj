(defproject clojuroids "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.badlogicgames.gdx/gdx "1.9.4"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.9.4"]
                 [com.badlogicgames.gdx/gdx-platform "1.9.4"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-freetype "1.9.4"]
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.9.4"
                  :classifier "natives-desktop"]
                 [org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot clojuroids.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
