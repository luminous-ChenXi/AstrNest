
import { useColorMode } from '@vueuse/core'

export const useTheme = () => {
  const mode = useColorMode({
    selector: 'html',
    attribute: 'class',
    emitAuto: true, // This is important to react to system changes
  })

  return {
    mode, // ref with value 'light', 'dark', or 'auto'
  }
}
