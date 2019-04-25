package net.simonvt.cathode

import android.content.Context
import net.simonvt.cathode.provider.generated.CathodeDatabase
import java.lang.reflect.InvocationTargetException

object DatabaseHelper {

  fun getInstance(context: Context): CathodeDatabase {
    try {
      val constructor = CathodeDatabase::class.java.getDeclaredConstructor(Context::class.java)
      constructor.isAccessible = true
      return constructor.newInstance(context)
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    } catch (e: InstantiationException) {
      throw RuntimeException(e)
    } catch (e: NoSuchMethodException) {
      throw RuntimeException(e)
    } catch (e: InvocationTargetException) {
      throw RuntimeException(e)
    }
  }
}
